package ru.gravit.launcher.guard;

import java.nio.file.Path;

public interface LauncherGuardInterface {
    String getName();
    Path getJavaBinPath();
    void init(boolean clientInstance);
}
