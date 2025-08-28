package pro.gravit.launcher.base.request.update;

import pro.gravit.launcher.base.events.request.ProfilesRequestEvent;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.websockets.WebSocketRequest;

public final class ProfilesRequest extends Request<ProfilesRequestEvent> implements WebSocketRequest {

    @Override
    public String getType() {
        return "profiles";
    }
}
