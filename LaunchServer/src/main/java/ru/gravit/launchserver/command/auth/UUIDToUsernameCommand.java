package ru.gravit.launchserver.command.auth;

import java.io.IOException;
import java.util.UUID;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.launchserver.command.CommandException;
import ru.gravit.utils.helper.LogHelper;

public final class UUIDToUsernameCommand extends Command {
    public UUIDToUsernameCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "<uuid>";
    }

    @Override
    public String getUsageDescription() {
        return "Convert player UUID to username";
    }

    @Override
    public void invoke(String... args) throws CommandException, IOException {
        verifyArgs(args, 1);
        UUID uuid = parseUUID(args[0]);

        // Get UUID by username
        String username = server.config.authHandler[0].uuidToUsername(uuid);
        if (username == null)
            throw new CommandException("Unknown UUID: " + uuid);

        // Print username
        LogHelper.subInfo("Username of player %s: '%s'", uuid, username);
    }
}
