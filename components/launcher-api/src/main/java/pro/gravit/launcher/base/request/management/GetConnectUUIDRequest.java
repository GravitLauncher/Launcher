package pro.gravit.launcher.base.request.management;

import pro.gravit.launcher.base.events.request.GetConnectUUIDRequestEvent;
import pro.gravit.launcher.base.request.Request;

public class GetConnectUUIDRequest extends Request<GetConnectUUIDRequestEvent> {
    @Override
    public String getType() {
        return "getConnectUUID";
    }
}
