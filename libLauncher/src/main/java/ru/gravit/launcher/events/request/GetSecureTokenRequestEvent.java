package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.request.ResultInterface;

public class GetSecureTokenRequestEvent implements ResultInterface {
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
