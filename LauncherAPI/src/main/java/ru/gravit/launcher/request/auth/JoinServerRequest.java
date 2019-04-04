package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.request.JoinServerRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
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
    public JoinServerRequest(String username, String accessToken, String serverID) {
        this.username = VerifyHelper.verifyUsername(username);
        this.accessToken = SecurityHelper.verifyToken(accessToken);
        this.serverID = VerifyHelper.verifyServerID(serverID);
    }

    @Override
    public JoinServerRequestEvent requestDo() throws IOException, InterruptedException {
        return (JoinServerRequestEvent) LegacyRequestBridge.sendRequest(this);
    }

    @Override
    public String getType() {
        return "joinServer";
    }
}
