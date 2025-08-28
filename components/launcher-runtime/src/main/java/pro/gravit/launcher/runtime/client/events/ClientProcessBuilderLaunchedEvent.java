package pro.gravit.launcher.runtime.client.events;

import pro.gravit.launcher.runtime.client.ClientLauncherProcess;
import pro.gravit.launcher.base.modules.LauncherModule;

public class ClientProcessBuilderLaunchedEvent extends LauncherModule.Event {
    public final ClientLauncherProcess processBuilder;

    public ClientProcessBuilderLaunchedEvent(ClientLauncherProcess processBuilder) {
        this.processBuilder = processBuilder;
    }
}
