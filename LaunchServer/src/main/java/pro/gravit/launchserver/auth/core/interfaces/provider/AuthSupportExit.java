package pro.gravit.launchserver.auth.core.interfaces.provider;

import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;

public interface AuthSupportExit extends AuthSupport {
    boolean deleteSession(UserSession session);

    boolean exitUser(User user);
}
