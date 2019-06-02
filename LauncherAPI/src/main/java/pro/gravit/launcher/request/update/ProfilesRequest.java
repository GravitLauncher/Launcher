package pro.gravit.launcher.request.update;

import pro.gravit.launcher.events.request.ProfilesRequestEvent;
import pro.gravit.launcher.request.websockets.RequestInterface;
import pro.gravit.launcher.request.Request;

public final class ProfilesRequest extends Request<ProfilesRequestEvent> implements RequestInterface {

    @Override
    public String getType() {
        return "profiles";
    }
}
