package ru.gravit.launchserver.command.auth;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthProviderPair;
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
        if(args.length > 2) pair = server.config.getAuthProviderPair(args[2]);
        else pair = server.config.getAuthProviderPair();
        if(pair == null) throw new IllegalStateException(String.format("Auth %s not found", args[1]));

        String login = args[0];
        String password = args[1];

        // Authenticate
        AuthProvider provider = pair.provider;
        AuthProviderResult result = provider.auth(login, password, "127.0.0.1");
        UUID uuid = pair.handler.auth(result);

        // Print auth successful message
        LogHelper.subInfo("UUID: %s, Username: '%s', Access Token: '%s'", uuid, result.username, result.accessToken);
    }
}
