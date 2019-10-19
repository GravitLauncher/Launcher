package pro.gravit.launcher.request.auth.password;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.request.auth.AuthRequest;

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
