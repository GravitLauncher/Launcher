package pro.gravit.launchserver.auth.core.interfaces.provider;

import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launchserver.auth.core.User;

import java.util.Map;

public interface AuthSupportRegistration {
    User registration(String login, AuthRequest.AuthPasswordInterface password, Map<String, String> properties);
}
