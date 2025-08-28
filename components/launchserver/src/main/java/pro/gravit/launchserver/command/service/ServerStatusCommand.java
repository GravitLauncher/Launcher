package pro.gravit.launchserver.command.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.command.CommandHandler;
import pro.gravit.utils.helper.JVMHelper;

public class ServerStatusCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

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
        logger.info("Show server status");
        logger.info("Memory: free {} | total: {} | max: {}", JVMHelper.RUNTIME.freeMemory(), JVMHelper.RUNTIME.totalMemory(), JVMHelper.RUNTIME.maxMemory());
        long uptime = JVMHelper.RUNTIME_MXBEAN.getUptime() / 1000;
        long second = uptime % 60;
        long min = (uptime / 60) % 60;
        long hour = (uptime / 60 / 60) % 24;
        long days = (uptime / 60 / 60 / 24);
        logger.info("Uptime: {} days {} hours {} minutes {} seconds", days, hour, min, second);
        logger.info("Uptime (double): {}", (double) JVMHelper.RUNTIME_MXBEAN.getUptime() / 1000);
        int commands = server.commandHandler.getBaseCategory().commandsMap().size();
        for (CommandHandler.Category category : server.commandHandler.getCategories()) {
            commands += category.category.commandsMap().size();
        }
        logger.info("Commands: {}({} categories)", commands, server.commandHandler.getCategories().size() + 1);
        for (AuthProviderPair pair : server.config.auth.values()) {
        }

    }
}
