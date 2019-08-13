package pro.gravit.launchserver.dao;

import java.util.List;
import java.util.UUID;

public class UserService {

    private final UserDAO usersDao;

    public UserService(UserDAO usersDAO) {
        this.usersDao = usersDAO;
    }

    public User findUser(int id) {
        return usersDao.findById(id);
    }

    public User findUserByUsername(String username) {
        return usersDao.findByUsername(username);
    }
    public User findUserByUUID(UUID uuid) {
        return usersDao.findByUUID(uuid);
    }

    public User registerNewUser(String username, String password)
    {
        User user = new User();
        user.username = username;
        user.setPassword(password);
        user.uuid = UUID.randomUUID();
        return user;
    }

    public void saveUser(User user) {
        usersDao.save(user);
    }

    public void deleteUser(User user) {
        usersDao.delete(user);
    }

    public void updateUser(User user) {
        usersDao.update(user);
    }

    public List<User> findAllUsers() {
        return usersDao.findAll();
    }


}