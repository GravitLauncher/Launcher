package pro.gravit.launchserver.auth.core.interfaces.provider;

import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.manangers.AuthManager;

import java.io.IOException;

public interface AuthSupportSudo {
    AuthManager.AuthReport sudo(User user, boolean shadow) throws IOException;
}
