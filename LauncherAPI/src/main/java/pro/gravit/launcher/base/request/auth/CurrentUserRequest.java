package pro.gravit.launcher.base.request.auth;

import pro.gravit.launcher.base.events.request.CurrentUserRequestEvent;
import pro.gravit.launcher.base.request.Request;

public class CurrentUserRequest extends Request<CurrentUserRequestEvent> {
    @Override
    public String getType() {
        return "currentUser";
    }
}
