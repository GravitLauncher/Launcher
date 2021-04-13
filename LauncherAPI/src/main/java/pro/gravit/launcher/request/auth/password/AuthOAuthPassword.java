package pro.gravit.launcher.request.auth.password;

import pro.gravit.launcher.request.auth.AuthRequest;

public class AuthOAuthPassword implements AuthRequest.AuthPasswordInterface {
    public final String accessToken;
    public final String refreshToken;
    public final int expire;

    public AuthOAuthPassword(String accessToken, String refreshToken, int expire) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expire = expire;
    }

    public AuthOAuthPassword(String accessToken, int expire) {
        this.accessToken = accessToken;
        this.refreshToken = null;
        this.expire = expire;
    }

    public AuthOAuthPassword(String accessToken) {
        this.accessToken = accessToken;
        this.refreshToken = null;
        this.expire = 0;
    }

    @Override
    public boolean check() {
        return true;
    }
}
