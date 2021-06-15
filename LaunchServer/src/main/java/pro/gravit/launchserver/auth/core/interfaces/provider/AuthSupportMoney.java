package pro.gravit.launchserver.auth.core.interfaces.provider;

import pro.gravit.launchserver.auth.Feature;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportMoney;

@Feature("money")
public interface AuthSupportMoney extends AuthSupport {
    default UserSupportMoney fetchUserMoney(User user) {
        return (UserSupportMoney) user;
    }
}
