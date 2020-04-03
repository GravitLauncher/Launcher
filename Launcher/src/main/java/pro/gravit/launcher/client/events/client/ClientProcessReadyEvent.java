package pro.gravit.launcher.client.events.client;

import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.client.ClientLauncherProcess;
import pro.gravit.launcher.modules.events.PostInitPhase;
import pro.gravit.launcher.profiles.ClientProfile;

public class ClientProcessReadyEvent extends PostInitPhase {
    public final LauncherEngine clientInstance;
    public final ClientLauncherProcess.ClientParams params;

    public ClientProcessReadyEvent(LauncherEngine clientInstance, ClientLauncherProcess.ClientParams params) {
        this.clientInstance = clientInstance;
        this.params = params;
    }
}
