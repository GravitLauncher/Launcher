package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.request.CheckServerRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.utils.helper.VerifyHelper;

public final class CheckServerRequest extends Request<CheckServerRequestEvent> implements RequestInterface {
    @LauncherNetworkAPI
    private final String username;
    @LauncherNetworkAPI
    private final String serverID;

    @LauncherAPI
    public CheckServerRequest(String username, String serverID) {
        this.username = VerifyHelper.verifyUsername(username);
        this.serverID = VerifyHelper.verifyServerID(serverID);
    }

    @Override
    public CheckServerRequestEvent requestDo() throws Exception
    {
        return (CheckServerRequestEvent) LegacyRequestBridge.sendRequest(this);
    }

    @Override
    public String getType() {
        return "checkServer";
    }
}
