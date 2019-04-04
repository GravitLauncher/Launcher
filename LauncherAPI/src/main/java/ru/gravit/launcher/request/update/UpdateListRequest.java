package ru.gravit.launcher.request.update;

import ru.gravit.launcher.events.request.UpdateListRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;

public final class UpdateListRequest extends Request<UpdateListRequestEvent> implements RequestInterface {

    @Override
    public UpdateListRequestEvent requestDo() throws Exception {
        return (UpdateListRequestEvent) LegacyRequestBridge.sendRequest(this);
    }

    @Override
    public String getType() {
        return "updateList";
    }
}
