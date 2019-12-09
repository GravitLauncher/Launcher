package pro.gravit.launcher.request;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.request.websockets.StandartClientWebSocketService;
import pro.gravit.launcher.request.websockets.WebSocketRequest;
import pro.gravit.utils.helper.SecurityHelper;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Request<R extends WebSocketEvent> implements WebSocketRequest {
    private static long session = SecurityHelper.secureRandom.nextLong();
    @LauncherNetworkAPI
    public final UUID requestUUID = UUID.randomUUID();
    public static StandartClientWebSocketService service;

    public static void setSession(long session) {
        Request.session = session;
    }

    public static long getSession() {
        return Request.session;
    }


    public static void requestError(String message) throws RequestException {
        throw new RequestException(message);
    }

    private transient final AtomicBoolean started = new AtomicBoolean(false);


    public R request() throws Exception {
        if (!started.compareAndSet(false, true))
            throw new IllegalStateException("Request already started");
        if (service == null)
            service = StandartClientWebSocketService.initWebSockets(Launcher.getConfig().address, false);
        return requestDo(service);
    }


    public R request(StandartClientWebSocketService service) throws Exception {
        if (!started.compareAndSet(false, true))
            throw new IllegalStateException("Request already started");
        return requestDo(service);
    }

    @SuppressWarnings("unchecked")
    protected R requestDo(StandartClientWebSocketService service) throws Exception {
        return (R) service.sendRequest(this);
    }

}
