package pro.gravit.launcher.request.auth;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.RestoreSessionRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.websockets.WebSocketRequest;

import java.util.UUID;

public class RestoreSessionRequest extends Request<RestoreSessionRequestEvent> implements WebSocketRequest {
    @LauncherNetworkAPI
    public final UUID session;
    public boolean needUserInfo;

    public RestoreSessionRequest(UUID session) {
        this.session = session;
    }

    public RestoreSessionRequest(UUID session, boolean needUserInfo) {
        this.session = session;
        this.needUserInfo = needUserInfo;
    }

    @Override
    public String getType() {
        return "restoreSession";
    }
}
