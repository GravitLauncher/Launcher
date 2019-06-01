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
        LogHelper.subInfo("Length of Cache "  + OAuthManager.getCacheLength());
        for(OAuthManager.Entry e:  OAuthManager.getStageArea() ){
            if(e.isInit())
                LogHelper.subInfo("IP is: " + e.getIP());
        }
    }
}
