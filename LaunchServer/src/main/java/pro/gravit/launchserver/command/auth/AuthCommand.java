package pro.gravit.launchserver.command.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.provider.AuthProviderResult;
import pro.gravit.launchserver.command.Command;

import java.util.UUID;

public final class AuthCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public AuthCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "<login> <password> <auth_id>";
    }

    @Override
    public String getUsageDescription() {
        return "Try to auth with specified login and password";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        AuthProviderPair pair;
        if (args.length > 2) pair = server.config.getAuthProviderPair(args[2]);
        else pair = server.config.getAuthProviderPair();
        if (pair == null) throw new IllegalStateException(String.format("Auth %s not found", args[1]));

        String login = args[0];
        String password = args[1];

        // Authenticate
        AuthProvider provider = pair.provider;
        AuthProviderResult result = provider.auth(login, new AuthPlainPassword(password), "127.0.0.1");
        UUID uuid = pair.handler.auth(result);

        // Print auth successful message
        logger.info("UUID: {}, Username: '{}', Access Token: '{}'", uuid, result.username, result.accessToken);
    }
}
