package pro.gravit.launcher.base.request.auth.password;

import pro.gravit.launcher.base.request.auth.AuthRequest;

public class AuthTOTPPassword implements AuthRequest.AuthPasswordInterface {
    public String totp;

    public AuthTOTPPassword() {
    }

    public AuthTOTPPassword(String totp) {
        this.totp = totp;
    }

    @Override
    public boolean check() {
        return true;
    }
}
