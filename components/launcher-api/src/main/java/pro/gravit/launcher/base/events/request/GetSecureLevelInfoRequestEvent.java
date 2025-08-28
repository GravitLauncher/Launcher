package pro.gravit.launcher.base.events.request;

import pro.gravit.launcher.base.events.RequestEvent;
import pro.gravit.launcher.core.api.features.HardwareVerificationFeatureAPI;

public class GetSecureLevelInfoRequestEvent extends RequestEvent implements HardwareVerificationFeatureAPI.SecurityLevelInfo {
    public final byte[] verifySecureKey;
    public boolean enabled;

    public GetSecureLevelInfoRequestEvent(byte[] verifySecureKey) {
        this.verifySecureKey = verifySecureKey;
    }

    public GetSecureLevelInfoRequestEvent(byte[] verifySecureKey, boolean enabled) {
        this.verifySecureKey = verifySecureKey;
        this.enabled = enabled;
    }

    @Override
    public String getType() {
        return "getSecureLevelInfo";
    }

    @Override
    public boolean isRequired() {
        return enabled;
    }

    @Override
    public byte[] getSignData() {
        return verifySecureKey;
    }
}
