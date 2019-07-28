package pro.gravit.launcher.request.uuid;

import pro.gravit.launcher.LauncherAPI;
import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.ProfileByUsernameRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.websockets.WebSocketRequest;
import pro.gravit.utils.helper.VerifyHelper;

public final class ProfileByUsernameRequest extends Request<ProfileByUsernameRequestEvent> implements WebSocketRequest {
    @LauncherNetworkAPI
    private final String username;

    @LauncherAPI
    public ProfileByUsernameRequest(String username) {
        this.username = VerifyHelper.verifyUsername(username);
    }

    @Override
    public String getType() {
        return "profileByUsername";
    }
}
