package pro.gravit.launcher.request.update;

import pro.gravit.launcher.events.request.UpdateListRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.websockets.RequestInterface;

public final class UpdateListRequest extends Request<UpdateListRequestEvent> implements RequestInterface {

    @Override
    public String getType() {
        return "updateList";
    }
}
