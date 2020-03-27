package pro.gravit.launcher.events.request;

import pro.gravit.launcher.events.RequestEvent;

public class VerifySecureLevelKeyRequestEvent extends RequestEvent {
    @Override
    public String getType() {
        return "verifySecureLevelKey";
    }
}
