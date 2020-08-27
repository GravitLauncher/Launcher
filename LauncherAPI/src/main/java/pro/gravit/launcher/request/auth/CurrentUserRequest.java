package pro.gravit.launcher.request.auth;

import pro.gravit.launcher.events.request.CurrentUserRequestEvent;
import pro.gravit.launcher.request.Request;

public class CurrentUserRequest extends Request<CurrentUserRequestEvent> {
    @Override
    public String getType() {
        return "currentUser";
    }
}
