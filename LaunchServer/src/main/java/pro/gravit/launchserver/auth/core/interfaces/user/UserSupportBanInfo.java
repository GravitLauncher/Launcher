package pro.gravit.launchserver.auth.core.interfaces.user;

import java.time.LocalDateTime;

public interface UserSupportBanInfo {
    interface UserBanInfo {
        String getId();

        default String getReason() {
            return null;
        }

        default String getModerator() {
            return null;
        }

        default LocalDateTime getStartDate() {
            return null;
        }

        default LocalDateTime getEndDate() {
            return null;
        }
    }

    UserBanInfo getBanInfo();
}
