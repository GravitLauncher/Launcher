package pro.gravit.launcher.request.secure;

import pro.gravit.launcher.events.request.GetSecureLevelInfoRequestEvent;
import pro.gravit.launcher.request.Request;

public class GetSecureLevelInfoRequest extends Request<GetSecureLevelInfoRequestEvent> {
    @Override
    public String getType() {
        return "getSecureLevelInfo";
    }
}
