package pro.gravit.launcher.server.setup;

import pro.gravit.launcher.modules.LauncherModule;

public class ServerWrapperPreSetupEvent extends LauncherModule.Event {
    public final ServerWrapperSetup setup;

    public ServerWrapperPreSetupEvent(ServerWrapperSetup setup) {
        this.setup = setup;
    }
}
