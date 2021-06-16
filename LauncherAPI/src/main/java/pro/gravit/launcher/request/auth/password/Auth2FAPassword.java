package pro.gravit.launcher.request.auth.password;

import pro.gravit.launcher.request.auth.AuthRequest;

public class Auth2FAPassword implements AuthRequest.AuthPasswordInterface {
    public AuthRequest.AuthPasswordInterface firstPassword;
    public AuthRequest.AuthPasswordInterface secondPassword;

    @Override
    public boolean check() {
        return firstPassword != null && firstPassword.check() && secondPassword != null && secondPassword.check();
    }

    @Override
    public boolean isAllowSave() {
        return firstPassword.isAllowSave() && secondPassword.isAllowSave();
    }
}
