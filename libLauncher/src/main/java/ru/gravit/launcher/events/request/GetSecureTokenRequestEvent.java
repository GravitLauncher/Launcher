package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.RequestEvent;
import ru.gravit.launcher.request.ResultInterface;

public class GetSecureTokenRequestEvent extends RequestEvent {
    @LauncherNetworkAPI
    public String secureToken;
    @Override
    public String getType() {
        return "GetSecureToken";
    }

    public GetSecureTokenRequestEvent(String secureToken) {
        this.secureToken = secureToken;
    }
}
