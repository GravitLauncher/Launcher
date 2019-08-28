package pro.gravit.launcher.server;

import pro.gravit.launcher.modules.LauncherModule;

public class ServerWrapperInitPhase extends LauncherModule.Event {
    public final ServerWrapper serverWrapper;

    public ServerWrapperInitPhase(ServerWrapper serverWrapper) {
        this.serverWrapper = serverWrapper;
    }
}
