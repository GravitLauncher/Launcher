package pro.gravit.launcher.request.update;

import pro.gravit.launcher.events.request.ProfilesRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.websockets.RequestInterface;

public final class ProfilesRequest extends Request<ProfilesRequestEvent> implements RequestInterface {

    @Override
    public String getType() {
        return "profiles";
    }
}
