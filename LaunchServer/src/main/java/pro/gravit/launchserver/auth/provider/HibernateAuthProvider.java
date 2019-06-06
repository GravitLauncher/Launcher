package pro.gravit.launchserver.auth.provider;

import java.io.IOException;

import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.dao.User;
import pro.gravit.utils.helper.SecurityHelper;

public class HibernateAuthProvider extends AuthProvider {
    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws Exception {
        User user = srv.userService.findUserByUsername(login);
        if(user == null || !user.verifyPassword(password))
        {
            if(user ==null) throw new AuthException("Username incorrect");
            else throw new AuthException("Username or password incorrect");
        }
        return new AuthProviderResult(login, SecurityHelper.randomStringToken(), srv);
    }

    @Override
    public void close() throws IOException {

    }
}
