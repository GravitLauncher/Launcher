package pro.gravit.launcher.request.uuid;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.ProfileByUsernameRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.websockets.WebSocketRequest;

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
