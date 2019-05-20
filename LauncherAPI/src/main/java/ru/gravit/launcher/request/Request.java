package ru.gravit.launcher.request;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.launcher.request.websockets.StandartClientWebSocketService;
import ru.gravit.utils.helper.SecurityHelper;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Request<R extends ResultInterface> implements RequestInterface {
    private static long session = SecurityHelper.secureRandom.nextLong();
    @LauncherNetworkAPI
    public UUID requestUUID = UUID.randomUUID();
    public static StandartClientWebSocketService service;

    public static void setSession(long session) {
        Request.session = session;
    }

    public static long getSession() {
        return Request.session;
    }

    @LauncherAPI
    public static void requestError(String message) throws RequestException {
        throw new RequestException(message);
    }

    private transient final AtomicBoolean started = new AtomicBoolean(false);

    @LauncherAPI
    public R request() throws Exception {
        if (!started.compareAndSet(false, true))
            throw new IllegalStateException("Request already started");
        if (service == null)
            service = StandartClientWebSocketService.initWebSockets(Launcher.getConfig().address, false);
        return requestDo(service);
    }

    @LauncherAPI
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
