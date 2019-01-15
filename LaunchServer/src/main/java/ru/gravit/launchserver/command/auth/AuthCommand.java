package ru.gravit.launchserver.command.auth;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.helper.LogHelper;

import java.util.UUID;

public final class AuthCommand extends Command {
    public AuthCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "<login> <password>";
    }

    @Override
    public String getUsageDescription() {
        return "Try to auth with specified login and password";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        String login = args[0];
        String password = args[1];
        int auth_id = 0;
        if (args.length >= 3) auth_id = Integer.valueOf(args[3]);

        // Authenticate
        AuthProvider provider = server.config.authProvider[auth_id];
        AuthProviderResult result = provider.auth(login, password, "127.0.0.1");
        UUID uuid = provider.getAccociateHandler(auth_id).auth(result);

        // Print auth successful message
        LogHelper.subInfo("UUID: %s, Username: '%s', Access Token: '%s'", uuid, result.username, result.accessToken);
    }
}
