package pro.gravit.launcher.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.events.request.CurrentUserRequestEvent;
import pro.gravit.launcher.events.request.RefreshTokenRequestEvent;
import pro.gravit.launcher.events.request.RestoreRequestEvent;
import pro.gravit.launcher.request.auth.RefreshTokenRequest;
import pro.gravit.launcher.request.auth.RestoreRequest;
import pro.gravit.launcher.request.websockets.StdWebSocketService;
import pro.gravit.launcher.request.websockets.WebSocketRequest;
import pro.gravit.utils.helper.LogHelper;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public abstract class Request<R extends WebSocketEvent> implements WebSocketRequest {
    private static final List<ExtendedTokenCallback> extendedTokenCallbacks = new ArrayList<>(4);
    private static final List<BiConsumer<String, AuthRequestEvent.OAuthRequestEvent>> oauthChangeHandlers = new ArrayList<>(4);

    private static RequestService requestService;
    private static AuthRequestEvent.OAuthRequestEvent oauth;
    private static Map<String, ExtendedToken> extendedTokens;
    private static String authId;
    private static long tokenExpiredTime;
    private static ScheduledExecutorService executorService;
    @LauncherNetworkAPI
    public final UUID requestUUID = UUID.randomUUID();
    private transient final AtomicBoolean started = new AtomicBoolean(false);

    public static void startAutoRefresh() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                restore(false, true);
            } catch (Exception e) {
                LogHelper.error(e);
            }
        }, 60, 60, TimeUnit.SECONDS);
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
            extendedTokens = new HashMap<>();
        }
        extendedTokens.put(name, token);
    }

    public static void addAllExtendedToken(Map<String, ExtendedToken> map) {
        if (extendedTokens == null) {
            extendedTokens = new HashMap<>();
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
        return restore(false, false);
    }

    private static Map<String, String> getExpiredExtendedTokens() {
        Map<String, String> map = new HashMap<>();
        for(Map.Entry<String, ExtendedToken> e : extendedTokens.entrySet()) {
            if(e.getValue().expire != 0 && e.getValue().expire < System.currentTimeMillis()) {
                map.put(e.getKey(), e.getValue().token);
            }
        }
        return map;
    }

    public static synchronized RequestRestoreReport restore(boolean needUserInfo, boolean refreshOnly) throws Exception {
        boolean refreshed = false;
        RestoreRequest request;
        if (oauth != null) {
            if (isTokenExpired() || oauth.accessToken == null) {
                RefreshTokenRequest refreshRequest = new RefreshTokenRequest(authId, oauth.refreshToken);
                RefreshTokenRequestEvent event = refreshRequest.request();
                setOAuth(authId, event.oauth);
                refreshed = true;
            }
            request = new RestoreRequest(authId, oauth.accessToken, refreshOnly ? getExpiredExtendedTokens() : getStringExtendedTokens(), needUserInfo);
        } else {
            request = new RestoreRequest(authId, null, refreshOnly ? getExpiredExtendedTokens() : getStringExtendedTokens(), false);
        }
        if(refreshOnly && (request.extended == null || request.extended.isEmpty())) {
            return new RequestRestoreReport(refreshed, null, null);
        }
        RestoreRequestEvent event = request.request();
        List<String> invalidTokens = null;
        if (event.invalidTokens != null && event.invalidTokens.size() > 0) {
            boolean needRequest = false;
            Map<String, String> tokens = new HashMap<>();
            for (ExtendedTokenCallback cb : extendedTokenCallbacks) {
                for (String tokenName : event.invalidTokens) {
                    ExtendedToken newToken = cb.tryGetNewToken(tokenName);
                    if (newToken != null) {
                        needRequest = true;
                        tokens.put(tokenName, newToken.token);
                        addExtendedToken(tokenName, newToken);
                    }
                }
            }
            if (needRequest) {
                request = new RestoreRequest(authId, null, tokens, false);
                event = request.request();
                if (event.invalidTokens != null && event.invalidTokens.size() > 0) {
                    LogHelper.warning("Tokens %s not restored", String.join(",", event.invalidTokens));
                }
            }
            invalidTokens = event.invalidTokens;
        }
        return new RequestRestoreReport(refreshed, invalidTokens, event.userInfo);
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
            this.expire = expire;
        }
    }

}
