package pro.gravit.launcher.runtime.client.events;

import pro.gravit.launcher.runtime.client.ClientLauncherProcess;
import pro.gravit.launcher.base.modules.LauncherModule;

public class ClientProcessBuilderPreLaunchEvent extends LauncherModule.Event {
    public final ClientLauncherProcess processBuilder;

    public ClientProcessBuilderPreLaunchEvent(ClientLauncherProcess processBuilder) {
        this.processBuilder = processBuilder;
    }
}
