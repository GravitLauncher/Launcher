package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.request.AuthRequestEvent;
import ru.gravit.launcher.events.request.RestoreSessionRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;

import java.io.IOException;

public class RestoreSessionRequest  extends Request<RestoreSessionRequestEvent> implements RequestInterface {
    @LauncherNetworkAPI
    public long session;

    public RestoreSessionRequest(long session) {
        this.session = session;
    }
    @Override
    public RestoreSessionRequestEvent requestWebSockets() throws IOException, InterruptedException {
        return (RestoreSessionRequestEvent) LegacyRequestBridge.sendRequest(this);
    }

    @Override
    public Integer getLegacyType() {
        return 0;
    }

    @Override
    protected RestoreSessionRequestEvent requestDo(HInput input, HOutput output) throws Exception {
        return new RestoreSessionRequestEvent();
    }

    @Override
    public String getType() {
        return "restoreSession";
    }
}
