package ru.gravit.launcher.guard;

import ru.gravit.launcher.client.ClientLauncherContext;

import java.nio.file.Path;
import java.util.Collection;

public interface LauncherGuardInterface {
    String getName();
    Path getJavaBinPath();
    void init(boolean clientInstance);
    void addCustomParams(ClientLauncherContext context);
    void addCustomEnv(ClientLauncherContext context);
}
