package ru.gravit.launcher.request.update;

import ru.gravit.launcher.events.request.UpdateListRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.RequestInterface;

public final class UpdateListRequest extends Request<UpdateListRequestEvent> implements RequestInterface {

    @Override
    public String getType() {
        return "updateList";
    }
}
