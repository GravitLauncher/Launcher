package pro.gravit.launcher.request.auth.details;

import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;

public class AuthWebViewDetails implements GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails {
    public final String url;
    public final String redirectUrl;
    public final boolean canBrowser;
    public final boolean onlyBrowser;

    public AuthWebViewDetails(String url, String redirectUrl, boolean canBrowser, boolean onlyBrowser) {
        this.url = url;
        this.redirectUrl = redirectUrl;
        this.canBrowser = canBrowser;
        this.onlyBrowser = onlyBrowser;
    }

    public AuthWebViewDetails(String url, String redirectUrl) {
        this.url = url;
        this.redirectUrl = redirectUrl;
        this.canBrowser = true;
        this.onlyBrowser = false;
    }

    @Override
    public String getType() {
        return "webview";
    }
}
