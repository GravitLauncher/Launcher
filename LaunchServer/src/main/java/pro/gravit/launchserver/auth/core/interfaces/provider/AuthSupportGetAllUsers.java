package pro.gravit.launchserver.auth.core.interfaces.provider;

import pro.gravit.launchserver.auth.core.User;

import java.util.List;

public interface AuthSupportGetAllUsers {
    List<User> getAllUsers();
}
