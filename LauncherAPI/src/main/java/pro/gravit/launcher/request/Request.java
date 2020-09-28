package pro.gravit.launcher.request;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.request.websockets.StdWebSocketService;
import pro.gravit.launcher.request.websockets.WebSocketRequest;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Request<R extends WebSocketEvent> implements WebSocketRequest {
    public static StdWebSocketService service;
    private static UUID session = UUID.randomUUID();
    @LauncherNetworkAPI
    public final UUID requestUUID = UUID.randomUUID();
    private transient final AtomicBoolean started = new AtomicBoolean(false);

    public static UUID getSession() {
        return Request.session;
    }

    public static void setSession(UUID session) {
        Request.session = session;
    }

    public static void requestError(String message) throws RequestException {
        throw new RequestException(message);
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

}
