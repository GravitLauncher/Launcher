package pro.gravit.launcher.base.request.auth;

import pro.gravit.launcher.base.events.request.FetchClientProfileKeyRequestEvent;
import pro.gravit.launcher.base.request.Request;

public class FetchClientProfileKeyRequest extends Request<FetchClientProfileKeyRequestEvent> {
    public FetchClientProfileKeyRequest() {
    }

    @Override
    public String getType() {
        return "clientProfileKey";
    }
}
