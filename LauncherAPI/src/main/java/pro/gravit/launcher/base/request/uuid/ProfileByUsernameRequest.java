package pro.gravit.launcher.base.request.uuid;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.base.events.request.ProfileByUsernameRequestEvent;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.websockets.WebSocketRequest;

public final class ProfileByUsernameRequest extends Request<ProfileByUsernameRequestEvent> implements WebSocketRequest {
    @LauncherNetworkAPI
    public final String username;


    public ProfileByUsernameRequest(String username) {
        this.username = username;
    }

    @Override
    public String getType() {
        return "profileByUsername";
    }
}
