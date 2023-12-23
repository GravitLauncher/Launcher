package pro.gravit.launcher.base.request;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.base.events.request.AuthRequestEvent;
import pro.gravit.launcher.base.events.request.CurrentUserRequestEvent;
import pro.gravit.launcher.base.events.request.RefreshTokenRequestEvent;
import pro.gravit.launcher.base.events.request.RestoreRequestEvent;
import pro.gravit.launcher.base.request.auth.RefreshTokenRequest;
import pro.gravit.launcher.base.request.auth.RestoreRequest;
import pro.gravit.launcher.base.request.websockets.StdWebSocketService;
import pro.gravit.launcher.base.request.websockets.WebSocketRequest;
import pro.gravit.utils.helper.LogHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public abstract class Request<R extends WebSocketEvent> implements WebSocketRequest {
    private static final List<ExtendedTokenCallback> extendedTokenCallbacks = new ArrayList<>(4);
    private static final List<BiConsumer<String, AuthRequestEvent.OAuthRequestEvent>> oauthChangeHandlers = new ArrayList<>(4);

    private static volatile RequestService requestService;
    private static volatile AuthRequestEvent.OAuthRequestEvent oauth;
    private static volatile Map<String, ExtendedToken> extendedTokens;
    private static volatile String authId;
    private static volatile long tokenExpiredTime;
    private static volatile ScheduledExecutorService executorService;
    private static volatile boolean autoRefreshRunning;
    @LauncherNetworkAPI
    public final UUID requestUUID = UUID.randomUUID();
    private transient final AtomicBoolean started = new AtomicBoolean(false);

    public static synchronized void startAutoRefresh() {
        if(!autoRefreshRunning) {
            if(executorService == null) {
                executorService = Executors.newSingleThreadScheduledExecutor((t) -> {
                    Thread thread = new Thread(t);
                    thread.setName("AutoRefresh thread");
                    thread.setDaemon(true);
                    return thread;
                });
            }
            executorService.scheduleAtFixedRate(() -> {
                try {
                    restore(false, true, false);
                } catch (Exception e) {
                    LogHelper.error(e);
                }
            }, 5, 5, TimeUnit.SECONDS);
            autoRefreshRunning = true;
        }
    }

    public static RequestService getRequestService() {
        return requestService;
    }

    public static void setRequestService(RequestService service) {
        requestService = service;
    }

    public static boolean isAvailable() {
        return requestService != null;
    }

    public static void setOAuth(String authId, AuthRequestEvent.OAuthRequestEvent event) {
        oauth = event;
        Request.authId = authId;
        if (oauth != null && oauth.expire != 0) {
            tokenExpiredTime = System.currentTimeMillis() + oauth.expire;
        } else {
            tokenExpiredTime = 0;
        }
        for (BiConsumer<String, AuthRequestEvent.OAuthRequestEvent> handler : oauthChangeHandlers) {
            handler.accept(authId, event);
        }
    }

    public static AuthRequestEvent.OAuthRequestEvent getOAuth() {
        return oauth;
    }

    public static String getAuthId() {
        return authId;
    }

    public static Map<String, ExtendedToken> getExtendedTokens() {
        if (extendedTokens != null) {
            return Collections.unmodifiableMap(extendedTokens);
        } else {
            return null;
        }
    }

    public static Map<String, String> getStringExtendedTokens() {
        if(extendedTokens != null) {
            Map<String, String> map = new HashMap<>();
            for(Map.Entry<String, ExtendedToken> e : extendedTokens.entrySet()) {
                map.put(e.getKey(), e.getValue().token);
            }
            return map;
        } else {
            return null;
        }
    }

    public static void clearExtendedTokens() {
        if (extendedTokens != null) {
            extendedTokens.clear();
        }
    }

    public static void addExtendedToken(String name, ExtendedToken token) {
        if (extendedTokens == null) {
            extendedTokens = new ConcurrentHashMap<>();
        }
        extendedTokens.put(name, token);
    }

    public static void addAllExtendedToken(Map<String, ExtendedToken> map) {
        if (extendedTokens == null) {
            extendedTokens = new ConcurrentHashMap<>();
        }
        extendedTokens.putAll(map);
    }

    public static void setOAuth(String authId, AuthRequestEvent.OAuthRequestEvent event, long tokenExpiredTime) {
        oauth = event;
        Request.authId = authId;
        Request.tokenExpiredTime = tokenExpiredTime;
    }

    public static boolean isTokenExpired() {
        if (oauth == null) return true;
        if (tokenExpiredTime == 0) return false;
        return System.currentTimeMillis() > tokenExpiredTime;
    }

    public static long getTokenExpiredTime() {
        return tokenExpiredTime;
    }

    public static String getAccessToken() {
        return oauth == null ? null : oauth.accessToken;
    }

    public static String getRefreshToken() {
        return oauth == null ? null : oauth.refreshToken;
    }

    public static void reconnect() throws Exception {

        getRequestService().open();
        restore();
    }

    public static RequestRestoreReport restore() throws Exception {
        return restore(false, false, false);
    }

    private synchronized static Map<String, String> getExpiredExtendedTokens() {
        if(extendedTokens == null) {
            return new HashMap<>();
        }
        Set<String> set = new HashSet<>();
        for(Map.Entry<String, ExtendedToken> e : extendedTokens.entrySet()) {
            if(e.getValue().expire != 0 && e.getValue().expire < System.currentTimeMillis()) {
                set.add(e.getKey());
            }
        }
        if(set.isEmpty()) {
            return new HashMap<>();
        }
        return makeNewTokens(set);
    }

    public static synchronized RequestRestoreReport restore(boolean needUserInfo, boolean refreshOnly, boolean noRefresh) throws Exception {
        boolean refreshed = false;
        RestoreRequest request;
        if (oauth != null) {
            if(isTokenExpired() || oauth.accessToken == null) {
                if(noRefresh) {
                    oauth = null;
                } else {
                    RefreshTokenRequest refreshRequest = new RefreshTokenRequest(authId, oauth.refreshToken);
                    RefreshTokenRequestEvent event = refreshRequest.request();
                    setOAuth(authId, event.oauth);
                    refreshed = true;
                }
            }
        }
        if (oauth != null) {
            request = new RestoreRequest(authId, oauth.accessToken, refreshOnly ? getExpiredExtendedTokens() : getStringExtendedTokens(), needUserInfo);
        } else {
            request = new RestoreRequest(authId, null, refreshOnly ? getExpiredExtendedTokens() : getStringExtendedTokens(), false);
        }
        if(refreshOnly && (request.extended == null || request.extended.isEmpty())) {
            return new RequestRestoreReport(refreshed, null, null);
        }
        RestoreRequestEvent event = request.request();
        List<String> invalidTokens = null;
        if (event.invalidTokens != null && !event.invalidTokens.isEmpty()) {
            Map<String, String> tokens = makeNewTokens(event.invalidTokens);
            if (!tokens.isEmpty()) {
                request = new RestoreRequest(authId, null, tokens, false);
                event = request.request();
                if (event.invalidTokens != null && !event.invalidTokens.isEmpty()) {
                    LogHelper.warning("Tokens %s not restored", String.join(",", event.invalidTokens));
                }
            }
            invalidTokens = event.invalidTokens;
        }
        return new RequestRestoreReport(refreshed, invalidTokens, event.userInfo);
    }

    private synchronized static Map<String, String> makeNewTokens(Collection<String> keys) {
        Map<String, String> tokens = new HashMap<>();
        for (ExtendedTokenCallback cb : extendedTokenCallbacks) {
            for (String tokenName : keys) {
                ExtendedToken newToken = cb.tryGetNewToken(tokenName);
                if (newToken != null) {
                    tokens.put(tokenName, newToken.token);
                    addExtendedToken(tokenName, newToken);
                }
            }
        }
        return tokens;
    }

    public static void requestError(String message) throws RequestException {
        throw new RequestException(message);
    }

    public void addExtendedTokenCallback(ExtendedTokenCallback cb) {
        extendedTokenCallbacks.add(cb);
    }

    public void removeExtendedTokenCallback(ExtendedTokenCallback cb) {
        extendedTokenCallbacks.remove(cb);
    }

    public void addOAuthChangeHandler(BiConsumer<String, AuthRequestEvent.OAuthRequestEvent> eventHandler) {
        oauthChangeHandlers.add(eventHandler);
    }

    public void removeOAuthChangeHandler(BiConsumer<String, AuthRequestEvent.OAuthRequestEvent> eventHandler) {
        oauthChangeHandlers.remove(eventHandler);
    }

    public R request() throws Exception {
        if (!started.compareAndSet(false, true))
            throw new IllegalStateException("Request already started");
        if (!isAvailable()) {
            throw new RequestException("RequestService not initialized");
        }
        return requestDo(requestService);
    }

    @Deprecated
    public R request(StdWebSocketService service) throws Exception {
        if (!started.compareAndSet(false, true))
            throw new IllegalStateException("Request already started");
        return requestDo(service);
    }

    public R request(RequestService service) throws Exception {
        if (!started.compareAndSet(false, true))
            throw new IllegalStateException("Request already started");
        return requestDo(service);
    }

    protected R requestDo(RequestService service) throws Exception {
        return service.requestSync(this);
    }

    public interface ExtendedTokenCallback {
        ExtendedToken tryGetNewToken(String name);
    }

    public static class RequestRestoreReport {
        public final boolean refreshed;
        public final List<String> invalidExtendedTokens;
        public final CurrentUserRequestEvent.UserInfo userInfo;

        public RequestRestoreReport(boolean refreshed, List<String> invalidExtendedTokens, CurrentUserRequestEvent.UserInfo userInfo) {
            this.refreshed = refreshed;
            this.invalidExtendedTokens = invalidExtendedTokens;
            this.userInfo = userInfo;
        }
    }

    public static class ExtendedToken {
        public final String token;
        public final long expire;

        public ExtendedToken(String token, long expire) {
            this.token = token;
            long time = System.currentTimeMillis();
            this.expire = expire < time/2 ? time+expire : expire;
        }
    }

}
