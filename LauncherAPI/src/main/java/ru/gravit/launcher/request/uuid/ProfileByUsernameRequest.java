package ru.gravit.launcher.request.uuid;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.request.ProfileByUsernameRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;

public final class ProfileByUsernameRequest extends Request<ProfileByUsernameRequestEvent> implements RequestInterface {
    @LauncherNetworkAPI
    private final String username;

    @LauncherAPI
    public ProfileByUsernameRequest(String username) {
        this.username = VerifyHelper.verifyUsername(username);
    }

    @Override
    public ProfileByUsernameRequestEvent requestDo() throws IOException, InterruptedException {
        return (ProfileByUsernameRequestEvent) LegacyRequestBridge.sendRequest(this);
    }

    @Override
    public String getType() {
        return "profileByUsername";
    }
}
