package pro.gravit.launcher.base.request.auth;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.base.events.request.JoinServerRequestEvent;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.websockets.WebSocketRequest;
import pro.gravit.utils.helper.VerifyHelper;

import java.util.UUID;

public final class JoinServerRequest extends Request<JoinServerRequestEvent> implements WebSocketRequest {

    // Instance
    @LauncherNetworkAPI
    public final String username;
    @LauncherNetworkAPI
    public final UUID uuid;
    @LauncherNetworkAPI
    public final String accessToken;
    @LauncherNetworkAPI
    public final String serverID;


    public JoinServerRequest(String username, String accessToken, String serverID) {
        this.username = username;
        this.uuid = null;
        this.accessToken = accessToken;
        this.serverID = VerifyHelper.verifyServerID(serverID);
    }

    public JoinServerRequest(UUID uuid, String accessToken, String serverID) {
        this.username = null;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.serverID = serverID;
    }

    @Override
    public String getType() {
        return "joinServer";
    }
}
