package pro.gravit.launcher.base.request.auth.password;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.base.request.auth.AuthRequest;

public class AuthPlainPassword implements AuthRequest.AuthPasswordInterface {
    @LauncherNetworkAPI
    public final String password;

    public AuthPlainPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean check() {
        return true;
    }
}
