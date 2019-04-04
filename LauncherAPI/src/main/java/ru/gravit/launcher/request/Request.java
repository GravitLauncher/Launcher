package ru.gravit.launcher.request;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.utils.helper.SecurityHelper;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Request<R> {
    private static long session = SecurityHelper.secureRandom.nextLong();

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
        return requestDo();
    }

    protected abstract R requestDo() throws Exception;

}
