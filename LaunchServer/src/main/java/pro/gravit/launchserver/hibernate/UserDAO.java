package pro.gravit.launchserver.hibernate;

import java.util.List;

public interface UserDAO {
    User findById(int id);
    User findByUsername(String username);
    void save(User user);
    void update(User user);
    void delete(User user);
    List<User> findAll();
}
