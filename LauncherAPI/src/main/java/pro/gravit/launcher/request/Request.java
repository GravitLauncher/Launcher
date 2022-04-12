package pro.gravit.launcher.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.events.request.RefreshTokenRequestEvent;
import pro.gravit.launcher.events.request.RestoreRequestEvent;
import pro.gravit.launcher.request.auth.RefreshTokenRequest;
import pro.gravit.launcher.request.auth.RestoreRequest;
import pro.gravit.launcher.request.websockets.StdWebSocketService;
import pro.gravit.launcher.request.websockets.WebSocketRequest;
import pro.gravit.utils.helper.LogHelper;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public abstract class Request<R extends WebSocketEvent> implements WebSocketRequest {
    private static final List<ExtendedTokenCallback> extendedTokenCallbacks = new ArrayList<>(4);
    private static final List<BiConsumer<String, AuthRequestEvent.OAuthRequestEvent>> oauthChangeHandlers = new ArrayList<>(4);
    @Deprecated
    public static StdWebSocketService service;
    private static RequestService requestService;
    private static AuthRequestEvent.OAuthRequestEvent oauth;
    private static Map<String, String> extendedTokens;
    private static String authId;
    private static long tokenExpiredTime;
    @LauncherNetworkAPI
    public final UUID requestUUID = UUID.randomUUID();
    private transient final AtomicBoolean started = new AtomicBoolean(false);

    public static void setRequestService(RequestService service) {
        requestService = service;
        if(service instanceof StdWebSocketService) {
            Request.service = (StdWebSocketService) service;
        }
    }

    public static RequestService getRequestService() {
        return requestService;
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

    public static Map<String, String> getExtendedTokens() {
        if (extendedTokens != null) {
            return Collections.unmodifiableMap(extendedTokens);
        } else {
            return null;
        }
    }

    public static void clearExtendedTokens() {
        if (extendedTokens != null) {
            extendedTokens.clear();
        }
    }

    public static void addExtendedToken(String name, String token) {
        if (extendedTokens == null) {
            extendedTokens = new HashMap<>();
        }
        extendedTokens.put(name, token);
    }

    public static void addAllExtendedToken(Map<String, String> map) {
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

    public static RequestRestoreReport reconnect() throws Exception {
        service.open();
        return restore();
    }

    public static class RequestRestoreReport {
        public final boolean legacySession;
        public final boolean refreshed;
        public final List<String> invalidExtendedTokens;

        public RequestRestoreReport(boolean legacySession, boolean refreshed, List<String> invalidExtendedTokens) {
            this.legacySession = legacySession;
            this.refreshed = refreshed;
            this.invalidExtendedTokens = invalidExtendedTokens;
        }
    }

    public static RequestRestoreReport restore() throws Exception {
        boolean refreshed = false;
        RestoreRequest request;
        if(oauth != null) {
            if (isTokenExpired() || oauth.accessToken == null) {
                RefreshTokenRequest refreshRequest = new RefreshTokenRequest(authId, oauth.refreshToken);
                RefreshTokenRequestEvent event = refreshRequest.request();
                setOAuth(authId, event.oauth);
                refreshed = true;
            }
            request = new RestoreRequest(authId, oauth.accessToken, extendedTokens, false);
        } else {
            request = new RestoreRequest(authId, null, extendedTokens, false);
        }
        RestoreRequestEvent event = request.request();
        List<String> invalidTokens = null;
        if (event.invalidTokens != null && event.invalidTokens.size() > 0) {
            boolean needRequest = false;
            Map<String, String> tokens = new HashMap<>();
            for (ExtendedTokenCallback cb : extendedTokenCallbacks) {
                for (String tokenName : event.invalidTokens) {
                    String newToken = cb.tryGetNewToken(tokenName);
                    if (newToken != null) {
                        needRequest = true;
                        tokens.put(tokenName, newToken);
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
        return new RequestRestoreReport(false, refreshed, invalidTokens);
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
        if(!isAvailable()) {
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
        String tryGetNewToken(String name);
    }

}
