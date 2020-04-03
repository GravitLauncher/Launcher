package pro.gravit.launcher.client.events.client;

import pro.gravit.launcher.client.ClientLauncherProcess;
import pro.gravit.launcher.modules.LauncherModule;

public class ClientProcessBuilderParamsWrittedEvent extends LauncherModule.Event {
    public final ClientLauncherProcess process;

    public ClientProcessBuilderParamsWrittedEvent(ClientLauncherProcess process) {
        this.process = process;
    }
}
