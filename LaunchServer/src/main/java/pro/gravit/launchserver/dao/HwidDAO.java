package pro.gravit.launchserver.dao;

import pro.gravit.launcher.hwid.HWID;

import java.util.List;

public interface HwidDAO {
    UserHWID findById(long id);

    List<? extends UserHWID> findHWIDs(HWID hwid);

    void save(UserHWID user);

    void update(UserHWID user);

    void delete(UserHWID user);

    List<UserHWID> findAll();
}
