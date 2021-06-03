package pro.gravit.launchserver.auth.core.interfaces.provider;

import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportBanInfo;

public interface AuthSupportUserBan {
    void banUser(User user, String reason);

    default void banUser(User user) {
        banUser(user, null);
    }

    void unbanUser(User user);

    default UserSupportBanInfo fetchUserBanInfo(User user) {
        return (UserSupportBanInfo) user;
    }
}
