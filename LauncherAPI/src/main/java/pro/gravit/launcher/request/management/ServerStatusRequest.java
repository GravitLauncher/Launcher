package pro.gravit.launcher.request.management;

import pro.gravit.launcher.events.request.ServerStatusRequestEvent;
import pro.gravit.launcher.request.Request;

public class ServerStatusRequest extends Request<ServerStatusRequestEvent> {
    @Override
    public String getType() {
        return "serverStatus";
    }
}
