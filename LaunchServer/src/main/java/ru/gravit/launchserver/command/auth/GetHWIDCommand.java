package ru.gravit.launchserver.command.auth;

import ru.gravit.launcher.HWID;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.helper.LogHelper;

import java.util.List;

public class GetHWIDCommand extends Command {
    public GetHWIDCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[username]";
    }

    @Override
    public String getUsageDescription() {
        return "get HWID from username";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        List<HWID> target = server.config.hwidHandler.getHwid(args[0]);
        for(HWID hwid : target)
        {
            if(hwid == null)
            {
                LogHelper.error("HWID %s: null", args[0]);
                continue;
            }
            LogHelper.info("HWID %s: %s", args[0], hwid.toString());
        }
        LogHelper.info("Found %d HWID", target.size());
    }
}
