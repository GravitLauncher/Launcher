package pro.gravit.launcher.request.auth;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.CheckServerRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.websockets.WebSocketRequest;
import pro.gravit.utils.helper.VerifyHelper;

public final class CheckServerRequest extends Request<CheckServerRequestEvent> implements WebSocketRequest {
    @LauncherNetworkAPI
    public final String username;
    @LauncherNetworkAPI
    public final String serverID;


    public CheckServerRequest(String username, String serverID) {
        this.username = username;
        this.serverID = VerifyHelper.verifyServerID(serverID);
    }

    @Override
    public String getType() {
        return "checkServer";
    }
}
