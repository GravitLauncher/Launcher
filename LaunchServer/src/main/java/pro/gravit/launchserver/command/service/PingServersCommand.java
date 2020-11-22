package pro.gravit.launchserver.command.service;

import pro.gravit.launcher.request.management.PingServerReportRequest;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class PingServersCommand extends Command {
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
    public void invoke(String... args) throws Exception {
        server.pingServerManager.map.forEach((name, data) -> {
            LogHelper.info("[%s] online %d / %d", name, data.lastReport == null ? -1 : data.lastReport.playersOnline, data.lastReport == null ? -1 : data.lastReport.maxPlayers);
            if(data.lastReport != null && data.lastReport.users != null)
            {
                for(PingServerReportRequest.PingServerReport.UsernameInfo user : data.lastReport.users)
                {
                    LogHelper.subInfo("User %s", user.username == null ? "null" : user.username);
                }
            }
        });
    }
}
