package pro.gravit.launchserver.auth.core.interfaces.provider;

import pro.gravit.launchserver.auth.Feature;
import pro.gravit.launchserver.auth.core.User;

import java.util.List;

@Feature("users")
public interface AuthSupportGetAllUsers {
    List<User> getAllUsers();
}
