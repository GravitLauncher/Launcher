package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.RequestEvent;

public class VerifySecureTokenRequestEvent extends RequestEvent {
    @LauncherNetworkAPI
    public final boolean success;

    @Override
    public String getType() {
        return "verifySecureToken";
    }

    public VerifySecureTokenRequestEvent(boolean success) {
        this.success = success;
    }
}
