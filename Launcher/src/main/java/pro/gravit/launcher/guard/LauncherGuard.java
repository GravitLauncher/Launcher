package pro.gravit.launcher.guard;

import pro.gravit.launcher.client.ClientLauncherProcess;

public interface LauncherGuard {
    String getName();

    void applyGuardParams(ClientLauncherProcess process);
}
