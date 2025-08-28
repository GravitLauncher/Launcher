package pro.gravit.launcher.base.request.secure;

import pro.gravit.launcher.base.events.request.GetSecureLevelInfoRequestEvent;
import pro.gravit.launcher.base.request.Request;

public class GetSecureLevelInfoRequest extends Request<GetSecureLevelInfoRequestEvent> {
    @Override
    public String getType() {
        return "getSecureLevelInfo";
    }
}
