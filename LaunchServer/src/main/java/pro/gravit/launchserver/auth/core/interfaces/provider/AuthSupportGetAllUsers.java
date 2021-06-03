package pro.gravit.launchserver.auth.core.interfaces.provider;

import pro.gravit.launchserver.auth.Feature;
import pro.gravit.launchserver.auth.core.User;

@Feature("users")
public interface AuthSupportGetAllUsers {
    Iterable<User> getAllUsers();
}
