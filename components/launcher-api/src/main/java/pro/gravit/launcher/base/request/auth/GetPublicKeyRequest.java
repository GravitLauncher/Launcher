package pro.gravit.launcher.base.request.auth;

import pro.gravit.launcher.base.events.request.GetPublicKeyRequestEvent;
import pro.gravit.launcher.base.request.Request;

public class GetPublicKeyRequest extends Request<GetPublicKeyRequestEvent> {
    public GetPublicKeyRequest() {
    }

    @Override
    public String getType() {
        return "getPublicKey";
    }
}
