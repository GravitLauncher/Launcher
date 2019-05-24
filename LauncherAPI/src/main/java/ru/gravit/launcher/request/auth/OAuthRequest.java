package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.HWID;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.events.request.AuthRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.RequestInterface;

public final class OAuthRequest extends Request<AuthRequestEvent> implements RequestInterface {

    private HWID hwid;

    @LauncherAPI
    public OAuthRequest(HWID hwid)
    {
        this.hwid = hwid;
    }
    @Override
    public String getType() {
        return "oauth";
    }
}
