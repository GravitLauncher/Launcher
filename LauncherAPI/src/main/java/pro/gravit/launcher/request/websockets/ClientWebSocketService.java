package pro.gravit.launcher.request.websockets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.events.ExceptionEvent;
import pro.gravit.launcher.events.NotificationEvent;
import pro.gravit.launcher.events.SignalEvent;
import pro.gravit.launcher.events.request.*;
import pro.gravit.launcher.hasher.HashedEntry;
import pro.gravit.launcher.hasher.HashedEntryAdapter;
import pro.gravit.launcher.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.profiles.optional.triggers.OptionalTrigger;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.GetAvailabilityAuthRequest;
import pro.gravit.utils.ProviderMap;
import pro.gravit.utils.UniversalJsonAdapter;
import pro.gravit.utils.helper.LogHelper;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;

public abstract class ClientWebSocketService extends ClientJSONPoint {
    public static final ProviderMap<WebSocketEvent> results = new ProviderMap<>();
    public static final ProviderMap<WebSocketRequest> requests = new ProviderMap<>();
    public final Gson gson;
    public final Boolean onConnect;
    public OnCloseCallback onCloseCallback;
    public ReconnectCallback reconnectCallback;

    public ClientWebSocketService(String address) throws SSLException {
        super(createURL(address));
        this.gson = Launcher.gsonManager.gson;
        this.onConnect = true;
    }

    public static void appendTypeAdapters(GsonBuilder builder) {
        builder.registerTypeAdapter(HashedEntry.class, new HashedEntryAdapter());
        builder.registerTypeAdapter(WebSocketEvent.class, new UniversalJsonAdapter<>(ClientWebSocketService.results));
        builder.registerTypeAdapter(WebSocketRequest.class, new UniversalJsonAdapter<>(ClientWebSocketService.requests));
        builder.registerTypeAdapter(AuthRequest.AuthPasswordInterface.class, new UniversalJsonAdapter<>(AuthRequest.providers));
        builder.registerTypeAdapter(GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails.class, new UniversalJsonAdapter<>(GetAvailabilityAuthRequest.providers));
        builder.registerTypeAdapter(OptionalAction.class, new UniversalJsonAdapter<>(OptionalAction.providers));
        builder.registerTypeAdapter(OptionalTrigger.class, new UniversalJsonAdapter<>(OptionalTrigger.providers));
    }

    private static URI createURL(String address) {
        try {
            return new URI(address);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void onMessage(String message) {
        WebSocketEvent result = gson.fromJson(message, WebSocketEvent.class);
        eventHandle(result);
    }

    public abstract <T extends WebSocketEvent> void eventHandle(T event);

    @Override
    void onDisconnect() {
        LogHelper.info("WebSocket client disconnect");
        if (onCloseCallback != null) onCloseCallback.onClose(0, "unsupported param", !isClosed);
    }

    @Override
    void onOpen() {
        synchronized (onConnect) {
            onConnect.notifyAll();
        }
    }

    public void registerRequests() {

    }

    public void registerResults() {
        results.register("auth", AuthRequestEvent.class);
        results.register("checkServer", CheckServerRequestEvent.class);
        results.register("joinServer", JoinServerRequestEvent.class);
        results.register("launcher", LauncherRequestEvent.class);
        results.register("profileByUsername", ProfileByUsernameRequestEvent.class);
        results.register("profileByUUID", ProfileByUUIDRequestEvent.class);
        results.register("batchProfileByUsername", BatchProfileByUsernameRequestEvent.class);
        results.register("profiles", ProfilesRequestEvent.class);
        results.register("setProfile", SetProfileRequestEvent.class);
        results.register("updateList", UpdateListRequestEvent.class);
        results.register("error", ErrorRequestEvent.class);
        results.register("update", UpdateRequestEvent.class);
        results.register("restoreSession", RestoreSessionRequestEvent.class);
        results.register("log", LogEvent.class);
        results.register("getAvailabilityAuth", GetAvailabilityAuthRequestEvent.class);
        results.register("exception", ExceptionEvent.class);
        results.register("register", RegisterRequestEvent.class);
        results.register("notification", NotificationEvent.class);
        results.register("signal", SignalEvent.class);
        results.register("exit", ExitRequestEvent.class);
        results.register("getSecureLevelInfo", GetSecureLevelInfoRequestEvent.class);
        results.register("verifySecureLevelKey", VerifySecureLevelKeyRequestEvent.class);
        results.register("securityReport", SecurityReportRequestEvent.class);
        results.register("hardwareReport", HardwareReportRequestEvent.class);
        results.register("serverStatus", ServerStatusRequestEvent.class);
        results.register("pingServerReport", PingServerReportRequestEvent.class);
        results.register("pingServer", PingServerRequestEvent.class);
        results.register("currentUser", CurrentUserRequestEvent.class);
        results.register("features", FeaturesRequestEvent.class);
        results.register("refreshToken", RefreshTokenRequestEvent.class);
        results.register("restore", RestoreRequestEvent.class);
    }

    public void waitIfNotConnected() {
        /*if(!isOpen() && !isClosed() && !isClosing())
        {
            LogHelper.warning("WebSocket not connected. Try wait onConnect object");
            synchronized (onConnect)
            {
                try {
                    onConnect.wait(5000);
                } catch (InterruptedException e) {
                    LogHelper.error(e);
                }
            }
        }*/
    }

    public void sendObject(Object obj) throws IOException {
        waitIfNotConnected();
        if (ch == null || !ch.isActive()) reconnectCallback.onReconnect();
        //if(isClosed() && reconnectCallback != null)
        //    reconnectCallback.onReconnect();
        send(gson.toJson(obj, WebSocketRequest.class));
    }

    public void sendObject(Object obj, Type type) throws IOException {
        waitIfNotConnected();
        if (ch == null || !ch.isActive()) reconnectCallback.onReconnect();
        //if(isClosed() && reconnectCallback != null)
        //    reconnectCallback.onReconnect();
        send(gson.toJson(obj, type));
    }

    @FunctionalInterface
    public interface OnCloseCallback {
        void onClose(int code, String reason, boolean remote);
    }

    public interface ReconnectCallback {
        void onReconnect() throws IOException;
    }

    @FunctionalInterface
    public interface EventHandler {
        /**
         * @param event processing event
         * @param <T>   event type
         * @return false - continue, true - stop
         */
        <T extends WebSocketEvent> boolean eventHandle(T event);
    }
}
