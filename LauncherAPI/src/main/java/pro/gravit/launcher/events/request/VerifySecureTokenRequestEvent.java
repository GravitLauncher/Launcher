package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherAPI;
import pro.gravit.launcher.events.RequestEvent;

public class VerifySecureTokenRequestEvent extends RequestEvent {
    @LauncherAPI
    public final boolean success;

    @Override
    public String getType() {
        return "verifySecureToken";
    }

    public VerifySecureTokenRequestEvent(boolean success) {
        this.success = success;
    }
}
