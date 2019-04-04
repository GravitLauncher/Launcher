package ru.gravit.launcher.request.update;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.events.request.ProfilesRequestEvent;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;

import java.util.ArrayList;
import java.util.List;

public final class ProfilesRequest extends Request<ProfilesRequestEvent> implements RequestInterface {

    @LauncherAPI
    public ProfilesRequest() {
        this(null);
    }

    @LauncherAPI
    public ProfilesRequest(LauncherConfig config) {
        super(config);
    }

    @Override
    public ProfilesRequestEvent requestWebSockets() throws Exception {
        return (ProfilesRequestEvent) LegacyRequestBridge.sendRequest(this);
    }

    @Override
    public String getType() {
        return "profiles";
    }
}
