package ru.gravit.launcher.server.setup;

import ru.gravit.utils.command.CommandHandler;
import ru.gravit.utils.command.JLineCommandHandler;
import ru.gravit.utils.command.StdCommandHandler;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;

public class ServerWrapperCommands {
    public final CommandHandler commandHandler;

    public void registerCommands() {
        //FUTURE
    }

    public ServerWrapperCommands(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    public ServerWrapperCommands() throws IOException {
        // Set command handler
        CommandHandler localCommandHandler;
        try {
            Class.forName("org.jline.terminal.Terminal");

            // JLine2 available
            localCommandHandler = new JLineCommandHandler();
            LogHelper.info("JLine2 terminal enabled");
        } catch (ClassNotFoundException ignored) {
            localCommandHandler = new StdCommandHandler(true);
            LogHelper.warning("JLine2 isn't in classpath, using std");
        }
        commandHandler = localCommandHandler;
    }
}
