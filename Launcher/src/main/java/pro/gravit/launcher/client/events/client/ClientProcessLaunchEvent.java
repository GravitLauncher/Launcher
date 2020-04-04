package pro.gravit.launcher.client.events.client;

import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.client.ClientLauncherProcess;
import pro.gravit.launcher.modules.LauncherModule;

public class ClientProcessLaunchEvent extends LauncherModule.Event {
    public final LauncherEngine clientInstance;
    public final ClientLauncherProcess.ClientParams params;

    public ClientProcessLaunchEvent(LauncherEngine clientInstance, ClientLauncherProcess.ClientParams params) {
        this.clientInstance = clientInstance;
        this.params = params;
    }
}
