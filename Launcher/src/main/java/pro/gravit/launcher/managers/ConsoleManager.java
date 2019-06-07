package pro.gravit.launcher.managers;

import java.io.IOException;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.console.FeatureCommand;
import pro.gravit.launcher.console.UnlockCommand;
import pro.gravit.launcher.console.admin.ExecCommand;
import pro.gravit.launcher.console.admin.LogListenerCommand;
import pro.gravit.launcher.console.store.CopyStoreDirCommand;
import pro.gravit.launcher.console.store.LinkStoreDirCommand;
import pro.gravit.launcher.console.store.StoreListCommand;
import pro.gravit.utils.command.BaseCommandCategory;
import pro.gravit.utils.command.CommandHandler;
import pro.gravit.utils.command.JLineCommandHandler;
import pro.gravit.utils.command.StdCommandHandler;
import pro.gravit.utils.command.basic.ClearCommand;
import pro.gravit.utils.command.basic.DebugCommand;
import pro.gravit.utils.command.basic.GCCommand;
import pro.gravit.utils.command.basic.HelpCommand;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.LogHelper;

public class ConsoleManager {
    public static CommandHandler handler;
    public static Thread thread;
    public static boolean isConsoleUnlock = false;

    public static void initConsole() throws IOException {
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
        handler = localCommandHandler;
        registerCommands();
        thread = CommonHelper.newThread("Launcher Console", true, handler);
        thread.start();
    }

    public static void registerCommands() {
        handler.registerCommand("help", new HelpCommand(handler));
        handler.registerCommand("gc", new GCCommand());
        handler.registerCommand("clear", new ClearCommand(handler));
        handler.registerCommand("unlock", new UnlockCommand());
    }

    public static boolean checkUnlockKey(String key) {
        return key.equals(Launcher.getConfig().oemUnlockKey);
    }

    public static void unlock() {
        handler.registerCommand("debug", new DebugCommand());
        handler.registerCommand("feature", new FeatureCommand());
        BaseCommandCategory admin = new BaseCommandCategory();
        admin.registerCommand("exec", new ExecCommand());
        admin.registerCommand("logListen", new LogListenerCommand());
        handler.registerCategory(new CommandHandler.Category(admin, "admin", "Server admin commands"));
        BaseCommandCategory store = new BaseCommandCategory();
        store.registerCommand("storeList", new StoreListCommand());
        store.registerCommand("copyStoreDir", new CopyStoreDirCommand());
        store.registerCommand("linkStoreDir", new LinkStoreDirCommand());
        handler.registerCategory(new CommandHandler.Category(admin, "store", "Store admin commands"));
        isConsoleUnlock = true;
    }
}
