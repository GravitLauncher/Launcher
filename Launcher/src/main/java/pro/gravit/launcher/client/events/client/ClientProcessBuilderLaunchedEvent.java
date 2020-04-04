package pro.gravit.launcher.client.events.client;

import pro.gravit.launcher.client.ClientLauncherProcess;
import pro.gravit.launcher.modules.LauncherModule;

public class ClientProcessBuilderLaunchedEvent extends LauncherModule.Event {
    public final ClientLauncherProcess processBuilder;

    public ClientProcessBuilderLaunchedEvent(ClientLauncherProcess processBuilder) {
        this.processBuilder = processBuilder;
    }
}
