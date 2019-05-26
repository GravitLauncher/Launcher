package ru.gravit.launchserver.command.handler;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.launchserver.manangers.OAuthManager;
import ru.gravit.utils.helper.LogHelper;

public final class OAuthCacheHandler extends Command {

    protected OAuthCacheHandler(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Debug all OAuthCache";
    }

    @Override
    public void invoke(String... args) {
        LogHelper.subInfo("Length of Cache "  + OAuthManager.stageAreaLength());
        for(int i=0; i < OAuthManager.stageAreaLength(); i++ ){
            LogHelper.subInfo("IP is: " + LaunchServer.server.cacheHandler.stageArea[i].IP());
        }
    }
}
