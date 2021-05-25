package pro.gravit.launchserver.auth.provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.SecurityHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class RejectAuthProvider extends AuthProvider implements Reconfigurable {
    private transient final Logger logger = LogManager.getLogger();
    public String message;
    public ArrayList<String> whitelist = new ArrayList<>();

    public RejectAuthProvider() {
    }

    public RejectAuthProvider(String message) {
        this.message = message;
    }

    @Override
    public AuthProviderResult auth(String login, AuthRequest.AuthPasswordInterface password, String ip) throws AuthException {
        if (whitelist != null) {
            for (String username : whitelist) {
                if (login.equals(username)) {
                    return new AuthProviderResult(login, SecurityHelper.randomStringToken(), ClientPermissions.DEFAULT);
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
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                message = args[0];
                logger.info("New reject message: {}", message);
            }
        });
        commands.put("whitelist.add", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                whitelist.add(args[0]);
                logger.info("{} added to whitelist", args[0]);
            }
        });
        return commands;
    }
}
