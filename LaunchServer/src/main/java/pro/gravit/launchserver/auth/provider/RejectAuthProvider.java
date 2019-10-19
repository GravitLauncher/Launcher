package pro.gravit.launchserver.auth.provider;

import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class RejectAuthProvider extends AuthProvider implements Reconfigurable {
    public RejectAuthProvider() {
    }

    public RejectAuthProvider(String message) {
        this.message = message;
    }

    private String message;
    private ArrayList<String> whitelist;

    @Override
    public AuthProviderResult auth(String login, AuthRequest.AuthPasswordInterface password, String ip) throws AuthException {
        if (whitelist != null) {
            for (String username : whitelist) {
                if (login.equals(username)) {
                    return new AuthProviderResult(login, SecurityHelper.randomStringToken(), srv);
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
    public Map<String, Command> getCommands() {
        Map<String, Command> commands = new HashMap<>();
        commands.put("message", new SubCommand() {
            @Override
            public void invoke(String... args) {
                message = args[0];
                LogHelper.info("New reject message: %s", message);
            }
        });
        return commands;
    }
}
