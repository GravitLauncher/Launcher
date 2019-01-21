package ru.gravit.launcher.guard;

import ru.gravit.launcher.client.DirBridge;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LauncherNoGuard implements LauncherGuardInterface {
    @Override
    public String getName() {
        return "noGuard";
    }

    @Override
    public Path getJavaBinPath() {
        return IOHelper.resolveJavaBin(Paths.get(System.getProperty("java.home")));
    }

    @Override
    public void init(boolean clientInstance) {
        LogHelper.warning("Using noGuard interface");
    }
}
