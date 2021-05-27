package pro.gravit.launcher.request.auth.password;

import pro.gravit.launcher.request.auth.AuthRequest;

public class AuthCodePassword implements AuthRequest.AuthPasswordInterface {
    public final String code;

    public AuthCodePassword(String code) {
        this.code = code;
    }

    @Override
    public boolean check() {
        return true;
    }
}
