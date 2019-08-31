package pro.gravit.launcher.server.setup;

import pro.gravit.launcher.modules.LauncherModule;

public class ServerWrapperPostSetupEvent extends LauncherModule.Event {
    public final ServerWrapperSetup setup;

    public ServerWrapperPostSetupEvent(ServerWrapperSetup setup) {
        this.setup = setup;
    }
}
