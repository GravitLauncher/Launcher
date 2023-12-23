package pro.gravit.launcher.runtime.client.events;

import pro.gravit.launcher.runtime.client.ClientLauncherProcess;
import pro.gravit.launcher.base.modules.LauncherModule;

public class ClientProcessBuilderParamsWrittedEvent extends LauncherModule.Event {
    public final ClientLauncherProcess process;

    public ClientProcessBuilderParamsWrittedEvent(ClientLauncherProcess process) {
        this.process = process;
    }
}
