package pro.gravit.launcher.request.auth;

import pro.gravit.launcher.events.request.RefreshTokenRequestEvent;
import pro.gravit.launcher.request.Request;

public class RefreshTokenRequest extends Request<RefreshTokenRequestEvent> {
    public String authId;
    public String refreshToken;

    public RefreshTokenRequest(String authId, String refreshToken) {
        this.authId = authId;
        this.refreshToken = refreshToken;
    }

    @Override
    public String getType() {
        return "refreshToken";
    }
}
