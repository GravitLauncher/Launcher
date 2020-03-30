package pro.gravit.launcher.guard;

import pro.gravit.launcher.client.ClientLauncherProcess;

public interface LauncherGuardInterface {
    String getName();

    void applyGuardParams(ClientLauncherProcess process);
}
