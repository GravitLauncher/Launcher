package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.request.JoinServerRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;

public final class JoinServerRequest extends Request<JoinServerRequestEvent> implements RequestInterface {

    // Instance
    @LauncherNetworkAPI
    private final String username;
    @LauncherNetworkAPI
    private final String accessToken;
    @LauncherNetworkAPI
    private final String serverID;

    @LauncherAPI
    public JoinServerRequest(LauncherConfig config, String username, String accessToken, String serverID) {
        super(config);
        this.username = VerifyHelper.verifyUsername(username);
        this.accessToken = SecurityHelper.verifyToken(accessToken);
        this.serverID = VerifyHelper.verifyServerID(serverID);
    }

    @Override
    public JoinServerRequestEvent requestWebSockets() throws IOException, InterruptedException {
        return (JoinServerRequestEvent) LegacyRequestBridge.sendRequest(this);
    }

    @LauncherAPI
    public JoinServerRequest(String username, String accessToken, String serverID) {
        this(null, username, accessToken, serverID);
    }
    @Override
    public String getType() {
        return "joinServer";
    }
}
