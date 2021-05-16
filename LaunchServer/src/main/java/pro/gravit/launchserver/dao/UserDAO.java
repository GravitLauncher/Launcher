package pro.gravit.launchserver.dao;

import java.util.List;
import java.util.UUID;

@Deprecated
public interface UserDAO {
    User findById(int id);

    User findByUsername(String username);

    User findByUUID(UUID uuid);

    void save(User user);

    void update(User user);

    void delete(User user);

    List<User> findAll();
}
