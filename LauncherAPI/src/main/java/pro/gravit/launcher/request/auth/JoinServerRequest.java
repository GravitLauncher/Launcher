package pro.gravit.launcher.request.auth;

import pro.gravit.launcher.LauncherAPI;
import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.JoinServerRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.websockets.RequestInterface;
import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.utils.helper.VerifyHelper;

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
    public String getType() {
        return "joinServer";
    }
}
