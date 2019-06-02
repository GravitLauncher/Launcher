package pro.gravit.launcher.request.uuid;

import pro.gravit.launcher.LauncherAPI;
import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.ProfileByUsernameRequestEvent;
import pro.gravit.launcher.request.websockets.RequestInterface;
import pro.gravit.utils.helper.VerifyHelper;
import pro.gravit.launcher.request.Request;

public final class ProfileByUsernameRequest extends Request<ProfileByUsernameRequestEvent> implements RequestInterface {
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
