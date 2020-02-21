package pro.gravit.launchserver.dao;

import pro.gravit.launcher.hwid.HWID;

public interface UserHWID {
    boolean isBanned();
    HWID toHWID();
}
