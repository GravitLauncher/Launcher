package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.RequestInterface;

public class GetAvailabilityAuthRequest extends Request<GetAvailabilityAuthRequestEvent> implements RequestInterface {

    @Override
    public String getType() {
        return "getAvailabilityAuth";
    }
}
