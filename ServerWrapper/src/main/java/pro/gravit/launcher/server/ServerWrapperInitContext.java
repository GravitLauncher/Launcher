package pro.gravit.launcher.server;

import pro.gravit.launcher.modules.LauncherInitContext;

public class ServerWrapperInitContext implements LauncherInitContext {
    public final ServerWrapper serverWrapper;

    public ServerWrapperInitContext(ServerWrapper serverWrapper) {
        this.serverWrapper = serverWrapper;
    }
}
