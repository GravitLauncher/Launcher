package pro.gravit.launchserver.modules.impl;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launchserver.LaunchServer;

public class LaunchServerInitContext implements LauncherInitContext {
    public final LaunchServer server;

    public LaunchServerInitContext(LaunchServer server) {
        this.server = server;
    }
}
