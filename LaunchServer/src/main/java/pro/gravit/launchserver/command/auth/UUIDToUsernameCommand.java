package pro.gravit.launchserver.command.auth;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.command.CommandException;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.util.UUID;

public final class UUIDToUsernameCommand extends Command {
    public UUIDToUsernameCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "<uuid> <auth_id>";
    }

    @Override
    public String getUsageDescription() {
        return "Convert player UUID to username";
    }

    @Override
    public void invoke(String... args) throws CommandException, IOException {
        verifyArgs(args, 1);
        AuthProviderPair pair;
        if (args.length > 1) pair = server.config.getAuthProviderPair(args[1]);
        else pair = server.config.getAuthProviderPair();
        if (pair == null) throw new IllegalStateException(String.format("Auth %s not found", args[1]));

        UUID uuid = parseUUID(args[0]);

        // Get UUID by username
        String username = pair.handler.uuidToUsername(uuid);
        if (username == null)
            throw new CommandException("Unknown UUID: " + uuid);

        // Print username
        LogHelper.subInfo("Username of player %s: '%s'", uuid, username);
    }
}
