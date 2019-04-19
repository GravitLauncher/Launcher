package ru.gravit.launcher.request.uuid;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.request.ProfileByUsernameRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.utils.helper.VerifyHelper;

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
