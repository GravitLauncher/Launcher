package pro.gravit.launchserver.auth.core.interfaces.session;

import pro.gravit.launchserver.auth.core.interfaces.UserHardware;

public interface UserSessionSupportHardware {
    String getHardwareId();
    UserHardware getHardware();
}
