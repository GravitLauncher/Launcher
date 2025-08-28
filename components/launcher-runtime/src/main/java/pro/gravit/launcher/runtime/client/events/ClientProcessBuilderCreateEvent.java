package pro.gravit.launcher.runtime.client.events;

import pro.gravit.launcher.runtime.client.ClientLauncherProcess;
import pro.gravit.launcher.base.modules.LauncherModule;

public class ClientProcessBuilderCreateEvent extends LauncherModule.Event {
    public final ClientLauncherProcess processBuilder;

    public ClientProcessBuilderCreateEvent(ClientLauncherProcess processBuilder) {
        this.processBuilder = processBuilder;
    }
}
