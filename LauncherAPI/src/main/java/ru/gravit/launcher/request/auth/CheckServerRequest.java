package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.request.CheckServerRequestEvent;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;

public final class CheckServerRequest extends Request<CheckServerRequestEvent> implements RequestInterface {
    @LauncherNetworkAPI
    private final String username;
    @LauncherNetworkAPI
    private final String serverID;

    @LauncherAPI
    public CheckServerRequest(LauncherConfig config, String username, String serverID) {
        super(config);
        this.username = VerifyHelper.verifyUsername(username);
        this.serverID = VerifyHelper.verifyServerID(serverID);
    }

    @LauncherAPI
    public CheckServerRequest(String username, String serverID) {
        this(null, username, serverID);
    }

    @Override
    public CheckServerRequestEvent requestWebSockets() throws Exception
    {
        return (CheckServerRequestEvent) LegacyRequestBridge.sendRequest(this);
    }

    @Override
    public String getType() {
        return "checkServer";
    }
}
