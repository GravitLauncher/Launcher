package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.request.ResultInterface;

public class VerifySecureTokenRequestEvent implements ResultInterface {
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
