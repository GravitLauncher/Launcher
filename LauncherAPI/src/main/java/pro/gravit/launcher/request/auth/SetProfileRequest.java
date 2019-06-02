package pro.gravit.launcher.request.auth;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.SetProfileRequestEvent;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.request.websockets.RequestInterface;
import pro.gravit.launcher.request.Request;

public class SetProfileRequest extends Request<SetProfileRequestEvent> implements RequestInterface {
    @LauncherNetworkAPI
    public String client;

    public SetProfileRequest(ClientProfile profile) {
        this.client = profile.getTitle();
    }

    @Override
    public String getType() {
        return "setProfile";
    }
}
