package pro.gravit.launcher.base.request.auth.details;

import pro.gravit.launcher.base.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.core.api.method.AuthMethodDetails;

public class AuthLoginOnlyDetails implements GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails {
    @Override
    public String getType() {
        return "loginonly";
    }

    @Override
    public AuthMethodDetails toAuthMethodDetails() {
        return new pro.gravit.launcher.core.api.method.details.AuthLoginOnlyDetails();
    }
}
