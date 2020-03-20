package pro.gravit.launcher.request.secure;

import pro.gravit.launcher.events.request.VerifySecureLevelKeyRequestEvent;
import pro.gravit.launcher.request.Request;

public class VerifySecureLevelKeyRequest extends Request<VerifySecureLevelKeyRequestEvent> {
    public final byte[] publicKey;
    public final byte[] signature;

    public VerifySecureLevelKeyRequest(byte[] publicKey, byte[] signature) {
        this.publicKey = publicKey;
        this.signature = signature;
    }

    @Override
    public String getType() {
        return "verifySecureLevelKey";
    }
}
