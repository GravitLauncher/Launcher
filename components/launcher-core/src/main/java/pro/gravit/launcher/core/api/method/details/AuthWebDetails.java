package pro.gravit.launcher.core.api.method.details;

import pro.gravit.launcher.core.api.method.AuthMethodDetails;

public record AuthWebDetails(String url, String redirectUrl, boolean externalBrowserSupport) implements AuthMethodDetails {
}
