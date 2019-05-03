package ru.gravit.launcher.request.update;

import ru.gravit.launcher.events.request.ProfilesRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.RequestInterface;

public final class ProfilesRequest extends Request<ProfilesRequestEvent> implements RequestInterface {

    @Override
    public String getType() {
        return "profiles";
    }
}
