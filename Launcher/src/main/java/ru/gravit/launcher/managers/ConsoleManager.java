package ru.gravit.launcher.managers;

import ru.gravit.utils.command.CommandHandler;
import ru.gravit.utils.command.JLineCommandHandler;
import ru.gravit.utils.command.StdCommandHandler;
import ru.gravit.utils.command.basic.HelpCommand;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;

public class ConsoleManager {
    public static CommandHandler handler;
    public static Thread thread;
    public static void initConsole() throws IOException
    {
        CommandHandler localCommandHandler;
        try {
            Class.forName("jline.Terminal");

            // JLine2 available
            localCommandHandler = new JLineCommandHandler();
            LogHelper.info("JLine2 terminal enabled");
        } catch (ClassNotFoundException ignored) {
            localCommandHandler = new StdCommandHandler(true);
            LogHelper.warning("JLine2 isn't in classpath, using std");
        }
        handler = localCommandHandler;
        registerCommands();
        thread = CommonHelper.newThread("Launcher Console", true, handler);
        thread.start();
    }
    public static void registerCommands()
    {
        handler.registerCommand("help", new HelpCommand(handler));
    }
}
