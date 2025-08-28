package pro.gravit.launcher.base.request.update;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.base.events.request.UpdateRequestEvent;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.websockets.WebSocketRequest;

public final class UpdateRequest extends Request<UpdateRequestEvent> implements WebSocketRequest {

    // Instance
    @LauncherNetworkAPI
    public final String dirName;

    public UpdateRequest(String dirName) {
        this.dirName = dirName;
    }

    @Override
    public String getType() {
        return "update";
    }
}
