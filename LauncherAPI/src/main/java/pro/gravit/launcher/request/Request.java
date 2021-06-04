package pro.gravit.launcher.request;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.events.request.RefreshTokenRequestEvent;
import pro.gravit.launcher.events.request.RestoreRequestEvent;
import pro.gravit.launcher.request.auth.RefreshTokenRequest;
import pro.gravit.launcher.request.auth.RestoreRequest;
import pro.gravit.launcher.request.auth.RestoreSessionRequest;
import pro.gravit.launcher.request.websockets.StdWebSocketService;
import pro.gravit.launcher.request.websockets.WebSocketRequest;
import pro.gravit.utils.helper.LogHelper;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public abstract class Request<R extends WebSocketEvent> implements WebSocketRequest {
    public static StdWebSocketService service;
    private static UUID session = UUID.randomUUID();
    private static AuthRequestEvent.OAuthRequestEvent oauth;
    private static Map<String, String> extendedTokens;
    private static String authId;
    private static long tokenExpiredTime;
    private static List<ExtendedTokenCallback> extendedTokenCallbacks = new ArrayList<>(4);
    private static List<BiConsumer<String, AuthRequestEvent.OAuthRequestEvent>> oauthChangeHandlers = new ArrayList<>(4);
    @LauncherNetworkAPI
    public final UUID requestUUID = UUID.randomUUID();
    private transient final AtomicBoolean started = new AtomicBoolean(false);

    public static UUID getSession() {
        return Request.session;
    }

    public static void setSession(UUID session) {
        Request.session = session;
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

    public static void reconnect() throws Exception {
        service.open();
        restore();
    }

    public static void restore() throws Exception {
        if (oauth != null) {
            if (isTokenExpired() || oauth.accessToken == null) {
                RefreshTokenRequest request = new RefreshTokenRequest(authId, oauth.refreshToken);
                RefreshTokenRequestEvent event = request.request();
                setOAuth(authId, event.oauth);
            }
            RestoreRequest request = new RestoreRequest(authId, oauth.accessToken, extendedTokens, false);
            RestoreRequestEvent event = request.request();
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
            }
        } else if (session != null) {
            RestoreSessionRequest request = new RestoreSessionRequest(session);
            request.request();
        }
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
        if (service == null)
            service = StdWebSocketService.initWebSockets(Launcher.getConfig().address, false);
        return requestDo(service);
    }

    public R request(StdWebSocketService service) throws Exception {
        if (!started.compareAndSet(false, true))
            throw new IllegalStateException("Request already started");
        return requestDo(service);
    }

    protected R requestDo(StdWebSocketService service) throws Exception {
        return service.requestSync(this);
    }

    public interface ExtendedTokenCallback {
        String tryGetNewToken(String name);
    }

}
