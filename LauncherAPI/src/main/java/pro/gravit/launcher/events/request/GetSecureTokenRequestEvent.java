package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.RequestEvent;

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
