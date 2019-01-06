package ru.gravit.launchserver.auth.provider;

import java.util.ArrayList;

import ru.gravit.launchserver.Reconfigurable;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;

public final class RejectAuthProvider extends AuthProvider implements Reconfigurable {
    public RejectAuthProvider() {
    }

    public RejectAuthProvider(String message) {
        this.message = message;
    }

    private String message;
    private ArrayList<String> whitelist;

    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws AuthException {
        if(whitelist != null)
        {
            for(String username : whitelist)
            {
                if(login.equals(username))
                {
                    return new AuthProviderResult(login, SecurityHelper.randomStringToken());
                }
            }
        }
        return authError(message);
    }

    @Override
    public void close() {
        // Do nothing
    }

    @Override
    public void reconfig(String action, String[] args) {
        if(action.equals("message"))
        {
            message = args[0];
            LogHelper.info("New reject message: %s", message);
        }
        else if(action.equals("whitelist.add"))
        {
            if(whitelist == null) whitelist = new ArrayList<>();
            whitelist.add(args[0]);
        }
        else if(action.equals("whitelist.remove"))
        {
            if(whitelist == null) whitelist = new ArrayList<>();
            whitelist.remove(args[0]);
        }
        else if(action.equals("whitelist.clear"))
        {
            whitelist.clear();
        }
    }

    @Override
    public void printConfigHelp() {
        LogHelper.info("message [new message] - set message");
        LogHelper.info("whitelist.add [username] - add username to whitelist");
        LogHelper.info("whitelist.remove [username] - remove username into whitelist");
        LogHelper.info("whitelist.clear - clear whitelist");
    }
}
