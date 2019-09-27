package pro.gravit.launcher.request.auth.password;

import pro.gravit.launcher.request.auth.AuthRequest;

public class AuthRSAPassword implements AuthRequest.AuthPasswordInterface {
    public final byte[] password;

    public AuthRSAPassword(byte[] password) {
        this.password = password;
    }

    @Override
    public boolean check() {
        return true;
    }
}
