package pro.gravit.launcher.request.auth;

import pro.gravit.launcher.events.request.GetPublicKeyRequestEvent;
import pro.gravit.launcher.request.Request;

public class GetPublicKeyRequest extends Request<GetPublicKeyRequestEvent> {
    public GetPublicKeyRequest() {
    }

    @Override
    public String getType() {
        return "getPublicKey";
    }
}
