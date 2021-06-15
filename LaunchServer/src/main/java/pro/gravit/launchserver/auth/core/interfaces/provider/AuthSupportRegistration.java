package pro.gravit.launchserver.auth.core.interfaces.provider;

import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launchserver.auth.Feature;
import pro.gravit.launchserver.auth.core.User;

import java.util.Map;

@Feature("registration")
public interface AuthSupportRegistration extends AuthSupport {
    User registration(String login, String email, AuthRequest.AuthPasswordInterface password, Map<String, String> properties);
}
