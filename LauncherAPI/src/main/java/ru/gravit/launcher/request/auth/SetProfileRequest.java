package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.request.SetProfileRequestEvent;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;

public class SetProfileRequest extends Request<SetProfileRequestEvent> implements RequestInterface {
    @LauncherNetworkAPI
    public String client;

    public SetProfileRequest(ClientProfile profile) {
        this.client = profile.getTitle();
    }

    @Override
    public SetProfileRequestEvent requestDo() throws Exception {
        return (SetProfileRequestEvent) LegacyRequestBridge.sendRequest(this);
    }

    @Override
    public String getType() {
        return "setProfile";
    }
}
