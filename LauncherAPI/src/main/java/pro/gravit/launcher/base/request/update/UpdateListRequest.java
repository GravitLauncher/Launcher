package pro.gravit.launcher.base.request.update;

import pro.gravit.launcher.base.events.request.UpdateListRequestEvent;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.websockets.WebSocketRequest;

public final class UpdateListRequest extends Request<UpdateListRequestEvent> implements WebSocketRequest {

    @Override
    public String getType() {
        return "updateList";
    }
}
