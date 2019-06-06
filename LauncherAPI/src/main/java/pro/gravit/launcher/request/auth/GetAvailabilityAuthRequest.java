package pro.gravit.launcher.request.auth;

import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.websockets.RequestInterface;

public class GetAvailabilityAuthRequest extends Request<GetAvailabilityAuthRequestEvent> implements RequestInterface {

    @Override
    public String getType() {
        return "getAvailabilityAuth";
    }
}
