package pro.gravit.launcher.base.request.auth.password;

import pro.gravit.launcher.base.request.auth.AuthRequest;

public class Auth2FAPassword implements AuthRequest.AuthPasswordInterface {
    public AuthRequest.AuthPasswordInterface firstPassword;
    public AuthRequest.AuthPasswordInterface secondPassword;

    public Auth2FAPassword() {
    }

    public Auth2FAPassword(AuthRequest.AuthPasswordInterface firstPassword, AuthRequest.AuthPasswordInterface secondPassword) {
        this.firstPassword = firstPassword;
        this.secondPassword = secondPassword;
    }

    @Override
    public boolean check() {
        return firstPassword != null && firstPassword.check() && secondPassword != null && secondPassword.check();
    }

    @Override
    public boolean isAllowSave() {
        return firstPassword.isAllowSave() && secondPassword.isAllowSave();
    }
}
