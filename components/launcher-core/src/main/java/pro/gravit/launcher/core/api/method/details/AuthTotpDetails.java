package pro.gravit.launcher.core.api.method.details;

import pro.gravit.launcher.core.api.method.AuthMethodDetails;

public record AuthTotpDetails(int length) implements AuthMethodDetails {
}
