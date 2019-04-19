package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.events.RequestEvent;
import ru.gravit.launcher.request.ResultInterface;

public class VerifySecureTokenRequestEvent extends RequestEvent {
    @LauncherAPI
    public boolean success;
    @Override
    public String getType() {
        return "verifySecureToken";
    }

    public VerifySecureTokenRequestEvent(boolean success) {
        this.success = success;
    }
}
