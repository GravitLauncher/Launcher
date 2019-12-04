package pro.gravit.launcher.request.update;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.UpdateRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.websockets.StandartClientWebSocketService;
import pro.gravit.launcher.request.websockets.WebSocketRequest;
import pro.gravit.utils.helper.LogHelper;

public final class UpdateRequest extends Request<UpdateRequestEvent> implements WebSocketRequest {

    public UpdateRequest(String dirName) {
        this.dirName = dirName;
    }

    @Override
    public String getType() {
        return "update";
    }



    @Override
    public UpdateRequestEvent requestDo(StandartClientWebSocketService service) throws Exception {
        LogHelper.debug("Start update request");
        return (UpdateRequestEvent) service.sendRequest(this);
    }

    // Instance
    @LauncherNetworkAPI
    private final String dirName;
}
