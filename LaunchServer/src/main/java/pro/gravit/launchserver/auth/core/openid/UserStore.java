package pro.gravit.launchserver.auth.core.openid;

import pro.gravit.launchserver.auth.core.User;

import java.util.UUID;

public interface UserStore {
    User getByUsername(String username);

    User getUserByUUID(UUID uuid);

    void createOrUpdateUser(User user);
}
