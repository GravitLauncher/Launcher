package pro.gravit.launcher.request.auth.password;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.request.auth.AuthRequest;

public class AuthECPassword implements AuthRequest.AuthPasswordInterface {
    @LauncherNetworkAPI
    public final byte[] password;

    public AuthECPassword(byte[] password) {
        this.password = password;
    }

    @Override
    public boolean check() {
        return true;
    }
}
