package pro.gravit.launcher.base.request.auth.details;

import pro.gravit.launcher.base.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.core.api.method.AuthMethodDetails;

public class AuthTotpDetails implements GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails {
    public final String alg;
    public final int maxKeyLength;

    public AuthTotpDetails(String alg, int maxKeyLength) {
        this.alg = alg;
        this.maxKeyLength = maxKeyLength;
    }

    public AuthTotpDetails(String alg) {
        this.alg = alg;
        this.maxKeyLength = 6;
    }

    @Override
    public String getType() {
        return "totp";
    }

    @Override
    public AuthMethodDetails toAuthMethodDetails() {
        return new pro.gravit.launcher.core.api.method.details.AuthTotpDetails(maxKeyLength);
    }
}
