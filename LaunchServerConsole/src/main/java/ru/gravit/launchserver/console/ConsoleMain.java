package ru.gravit.launchserver.console;

import ru.gravit.launcher.server.ServerWrapper;
import ru.gravit.utils.command.CommandHandler;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;

public class ConsoleMain {
    public static CommandHandler commandHandler;
    public static void main(String[] args) throws IOException {
        if(ServerWrapper.config == null)
        {
            LogHelper.warning("ServerWrapper not found");
        }
        if(!ServerWrapper.permissions.canAdmin)
        {
            LogHelper.warning("Permission canAdmin not found");
        }
        try {
            Class.forName("jline.Terminal");

            // JLine2 available
            commandHandler = new RemoteJLineCommandHandler();
            LogHelper.info("JLine2 terminal enabled");
        } catch (ClassNotFoundException ignored) {
            commandHandler = new RemoteStdCommandHandler(true);
            LogHelper.warning("JLine2 isn't in classpath, using std");
        }
        LogHelper.info("CommandHandler started. Use 'exit' to exit this console");
        commandHandler.run();
    }
}
