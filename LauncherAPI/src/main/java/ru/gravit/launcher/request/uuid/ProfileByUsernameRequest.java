package ru.gravit.launcher.request.uuid;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.request.ProfileByUsernameRequestEvent;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;

public final class ProfileByUsernameRequest extends Request<ProfileByUsernameRequestEvent> implements RequestInterface {
    @LauncherNetworkAPI
    private final String username;

    @LauncherAPI
    public ProfileByUsernameRequest(LauncherConfig config, String username) {
        super(config);
        this.username = VerifyHelper.verifyUsername(username);
    }

    @LauncherAPI
    public ProfileByUsernameRequest(String username) {
        this(null, username);
    }

    @Override
    public ProfileByUsernameRequestEvent requestWebSockets() throws IOException, InterruptedException {
        return (ProfileByUsernameRequestEvent) LegacyRequestBridge.sendRequest(this);
    }

    @Override
    public String getType() {
        return "profileByUsername";
    }
}
