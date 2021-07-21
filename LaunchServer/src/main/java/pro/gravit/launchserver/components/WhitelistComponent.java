package pro.gravit.launchserver.components;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.launchserver.socket.response.auth.JoinServerResponse;
import pro.gravit.utils.HookException;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.SubCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WhitelistComponent extends Component implements AutoCloseable, Reconfigurable {
    private transient final Logger logger = LogManager.getLogger();
    public String message = "auth.message.techwork";
    public boolean enabled = true;
    public List<String> whitelist = new ArrayList<>();
    private transient LaunchServer server;

    @Override
    public void init(LaunchServer launchServer) {
        this.server = launchServer;
        this.server.authHookManager.preHook.registerHook(this::hookAuth);
        this.server.authHookManager.joinServerHook.registerHook(this::hookJoin);
    }

    public boolean hookAuth(AuthResponse.AuthContext context, Client client) throws HookException {
        if (enabled) {
            if (!whitelist.contains(context.login)) {
                throw new HookException(message);
            }
        }
        return false;
    }

    public boolean hookJoin(JoinServerResponse response, Client client) throws HookException {
        if (enabled) {
            if (!whitelist.contains(response.username)) {
                throw new HookException(message);
            }
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        this.server.authHookManager.preHook.unregisterHook(this::hookAuth);
        this.server.authHookManager.joinServerHook.unregisterHook(this::hookJoin);
    }

    @Override
    public Map<String, Command> getCommands() {
        var commands = defaultCommandsMap();
        commands.put("setmessage", new SubCommand("[new message]", "set message") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                message = args[0];
                logger.info("Message: {}", args[0]);
            }
        });
        commands.put("whitelist.add", new SubCommand("[login]", "add login to whitelist") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                whitelist.add(args[0]);
                logger.info("{} added to whitelist", args[0]);
            }
        });
        commands.put("whitelist.remove", new SubCommand("[login]", "remove login from whitelist") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                whitelist.remove(args[0]);
                logger.info("{} removed from whitelist", args[0]);
            }
        });
        commands.put("disable", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                enabled = false;
                logger.info("Whitelist disabled");
            }
        });
        commands.put("enable", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                enabled = true;
                logger.info("Whitelist enabled");
            }
        });
        return commands;
    }
}
