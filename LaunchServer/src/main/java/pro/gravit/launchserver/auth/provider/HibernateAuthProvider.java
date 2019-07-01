package pro.gravit.launchserver.auth.provider;

import java.io.IOException;

import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.manangers.hook.AuthHookManager;
import pro.gravit.utils.helper.SecurityHelper;

public class HibernateAuthProvider extends AuthProvider {
    public boolean autoReg;
    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws Exception {
        User user = srv.config.dao.userService.findUserByUsername(login);
        if(user == null && autoReg)
        {
            AuthHookManager.RegContext context = new AuthHookManager.RegContext(login, password, ip, false);
            if(srv.authHookManager.registraion.hook(context))
            {
                user = srv.config.dao.userService.registerNewUser(login, password);
            }
            else
            {
                throw new AuthException("Registration canceled. Try again later");
            }
        }
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
