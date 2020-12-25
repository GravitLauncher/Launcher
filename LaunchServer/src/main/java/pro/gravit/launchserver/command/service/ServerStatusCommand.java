package pro.gravit.launchserver.command.service;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.handler.CachedAuthHandler;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.command.CommandHandler;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

public class ServerStatusCommand extends Command {
    public ServerStatusCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Check server status";
    }

    @Override
    public void invoke(String... args) {
        LogHelper.info("Show server status");
        LogHelper.info("Memory: free %d | total: %d | max: %d", JVMHelper.RUNTIME.freeMemory(), JVMHelper.RUNTIME.totalMemory(), JVMHelper.RUNTIME.maxMemory());
        long uptime = JVMHelper.RUNTIME_MXBEAN.getUptime() / 1000;
        long second = uptime % 60;
        long min = (uptime / 60) % 60;
        long hour = (uptime / 60 / 60) % 24;
        long days = (uptime / 60 / 60 / 24);
        LogHelper.info("Uptime: %d days %d hours %d minutes %d seconds", days, hour, min, second);
        LogHelper.info("Uptime (double): %f", (double) JVMHelper.RUNTIME_MXBEAN.getUptime() / 1000);
        int commands = server.commandHandler.getBaseCategory().commandsMap().size();
        for (CommandHandler.Category category : server.commandHandler.getCategories()) {
            commands += category.category.commandsMap().size();
        }
        LogHelper.info("Commands: %d(%d categories)", commands, server.commandHandler.getCategories().size() + 1);
        for (AuthProviderPair pair : server.config.auth.values()) {
            if (pair.handler instanceof CachedAuthHandler) {
                LogHelper.info("AuthHandler %s: EntryCache: %d | usernameCache: %d", pair.name, ((CachedAuthHandler) pair.handler).getEntryCache().size(), ((CachedAuthHandler) pair.handler).getUsernamesCache().size());
            }
        }

    }
}
