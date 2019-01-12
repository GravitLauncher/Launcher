package ru.gravit.launchserver.command.service;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.handler.CachedAuthHandler;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;

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
    public void invoke(String... args) throws Exception {
        LogHelper.info("Show server status");
        LogHelper.info("Memory: free %d | total: %d | max: %d", JVMHelper.RUNTIME.freeMemory(), JVMHelper.RUNTIME.totalMemory(),JVMHelper.RUNTIME.maxMemory());
        long uptime = JVMHelper.RUNTIME_MXBEAN.getUptime() / 1000;
        long second = uptime % 60;
        long min = (uptime / 60) % 60;
        long hour = (uptime / 60 / 60) % 24;
        long days = (uptime / 60 / 60 / 24);
        LogHelper.info("Uptime: %d days %d hours %d minutes %d seconds",days,hour,min,second);
        LogHelper.info("Uptime (double): %f", (double)JVMHelper.RUNTIME_MXBEAN.getUptime() / 1000);
        LogHelper.info("Sessions: %d | Modules: %d | Commands: %d",server.sessionManager.getSessions().size(), server.modulesManager.modules.size(), server.commandHandler.commandsMap().size());
        if(server.config.authHandler instanceof CachedAuthHandler)
        {
            LogHelper.info("AuthHandler: EntryCache: %d | usernameCache: %d",((CachedAuthHandler) server.config.authHandler).getEntryCache().size(),((CachedAuthHandler) server.config.authHandler).getUsernamesCache().size());
        }
    }
}
