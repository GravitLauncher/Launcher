package pro.gravit.launcher.request.auth;

import pro.gravit.launcher.events.request.FetchClientProfileKeyRequestEvent;
import pro.gravit.launcher.request.Request;

public class FetchClientProfileKeyRequest extends Request<FetchClientProfileKeyRequestEvent> {
    public FetchClientProfileKeyRequest() {
    }

    @Override
    public String getType() {
        return "clientProfileKey";
    }
}
