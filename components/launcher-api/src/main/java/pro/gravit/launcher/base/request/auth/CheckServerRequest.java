package pro.gravit.launcher.base.request.auth;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.base.events.request.CheckServerRequestEvent;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.websockets.WebSocketRequest;
import pro.gravit.utils.helper.VerifyHelper;

public final class CheckServerRequest extends Request<CheckServerRequestEvent> implements WebSocketRequest {
    @LauncherNetworkAPI
    public final String username;
    @LauncherNetworkAPI
    public final String serverID;
    @LauncherNetworkAPI
    public boolean needHardware;
    @LauncherNetworkAPI
    public boolean needProperties;


    public CheckServerRequest(String username, String serverID) {
        this.username = username;
        this.serverID = VerifyHelper.verifyServerID(serverID);
    }

    public CheckServerRequest(String username, String serverID, boolean needHardware, boolean needProperties) {
        this.username = username;
        this.serverID = VerifyHelper.verifyServerID(serverID);
        this.needHardware = needHardware;
        this.needProperties = needProperties;
    }

    @Override
    public String getType() {
        return "checkServer";
    }
}
