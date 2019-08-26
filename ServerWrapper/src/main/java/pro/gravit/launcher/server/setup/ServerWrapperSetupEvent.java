package pro.gravit.launcher.server.setup;

import pro.gravit.launcher.modules.LauncherModule;

public class ServerWrapperSetupEvent extends LauncherModule.Event {
    public final ServerWrapperSetup setup;

    public ServerWrapperSetupEvent(ServerWrapperSetup setup) {
        this.setup = setup;
    }
}
