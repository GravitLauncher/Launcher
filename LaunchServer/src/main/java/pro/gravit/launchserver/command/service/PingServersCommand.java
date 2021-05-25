package pro.gravit.launchserver.command.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.request.management.PingServerReportRequest;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class PingServersCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public PingServersCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[]";
    }

    @Override
    public String getUsageDescription() {
        return "show modern pings status";
    }

    @Override
    public void invoke(String... args) {
        server.pingServerManager.map.forEach((name, data) -> {
            logger.info("[{}] online {} / {}", name, data.lastReport == null ? -1 : data.lastReport.playersOnline, data.lastReport == null ? -1 : data.lastReport.maxPlayers);
            if (data.lastReport != null && data.lastReport.users != null) {
                for (PingServerReportRequest.PingServerReport.UsernameInfo user : data.lastReport.users) {
                    logger.info("User {}", user.username == null ? "null" : user.username);
                }
            }
        });
    }
}
