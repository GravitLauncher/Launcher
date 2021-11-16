package pro.gravit.launcher.events.request;

import pro.gravit.launcher.events.RequestEvent;

@Deprecated
public class RegisterRequestEvent extends RequestEvent {
    @Override
    public String getType() {
        return "register";
    }
}
