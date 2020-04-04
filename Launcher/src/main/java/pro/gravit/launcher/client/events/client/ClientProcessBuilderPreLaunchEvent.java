package pro.gravit.launcher.client.events.client;

import pro.gravit.launcher.client.ClientLauncherProcess;
import pro.gravit.launcher.modules.LauncherModule;

public class ClientProcessBuilderPreLaunchEvent extends LauncherModule.Event {
    public final ClientLauncherProcess processBuilder;

    public ClientProcessBuilderPreLaunchEvent(ClientLauncherProcess processBuilder) {
        this.processBuilder = processBuilder;
    }
}
