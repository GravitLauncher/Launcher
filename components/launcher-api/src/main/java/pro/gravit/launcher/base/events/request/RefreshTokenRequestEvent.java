package pro.gravit.launcher.base.events.request;

import pro.gravit.launcher.base.events.RequestEvent;

public class RefreshTokenRequestEvent extends RequestEvent {
    public AuthRequestEvent.OAuthRequestEvent oauth;

    public RefreshTokenRequestEvent(AuthRequestEvent.OAuthRequestEvent oauth) {
        this.oauth = oauth;
    }

    @Override
    public String getType() {
        return "refreshToken";
    }
}
