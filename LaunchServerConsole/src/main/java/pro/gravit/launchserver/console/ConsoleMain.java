package pro.gravit.launchserver.console;

import java.io.IOException;

import pro.gravit.launcher.server.ServerWrapper;
import pro.gravit.utils.command.CommandHandler;
import pro.gravit.utils.command.JLineCommandHandler;
import pro.gravit.utils.command.StdCommandHandler;
import pro.gravit.utils.command.basic.HelpCommand;
import pro.gravit.utils.helper.LogHelper;

public class ConsoleMain {
    public static CommandHandler commandHandler;

    public static void main(String[] args) throws IOException {
        if (ServerWrapper.wrapper.config == null) {
            LogHelper.warning("ServerWrapper not found");
        }
        if (!ServerWrapper.wrapper.permissions.canAdmin) {
            LogHelper.warning("Permission canAdmin not found");
        }
        try {
            Class.forName("org.jline.terminal.Terminal");

            // JLine2 available
            commandHandler = new JLineCommandHandler();
            LogHelper.info("JLine2 terminal enabled");
        } catch (ClassNotFoundException ignored) {
            commandHandler = new StdCommandHandler(true);
            LogHelper.warning("JLine2 isn't in classpath, using std");
        }
        registerCommands();
        LogHelper.info("CommandHandler started. Use 'exit' to exit this console");
        commandHandler.run();
    }

    public static void registerCommands() {
        commandHandler.registerCommand("help", new HelpCommand(commandHandler));
        commandHandler.registerCommand("exit", new ExitCommand());
        commandHandler.registerCommand("logListener", new LogListenerCommand());
        commandHandler.registerCommand("exec", new ExecCommand());
    }
}
