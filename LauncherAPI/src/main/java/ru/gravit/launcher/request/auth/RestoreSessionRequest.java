package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.request.RestoreSessionRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.RequestInterface;

public class RestoreSessionRequest  extends Request<RestoreSessionRequestEvent> implements RequestInterface {
    @LauncherNetworkAPI
    public long session;

    public RestoreSessionRequest(long session) {
        this.session = session;
    }

    @Override
    public String getType() {
        return "restoreSession";
    }
}
