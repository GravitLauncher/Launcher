package pro.gravit.launcher.request.auth;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.RestoreSessionRequestEvent;
import pro.gravit.launcher.request.websockets.RequestInterface;
import pro.gravit.launcher.request.Request;

public class RestoreSessionRequest extends Request<RestoreSessionRequestEvent> implements RequestInterface {
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
