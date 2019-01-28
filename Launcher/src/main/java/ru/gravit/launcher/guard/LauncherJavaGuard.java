package ru.gravit.launcher.guard;

import ru.gravit.launcher.client.ClientLauncher;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LauncherJavaGuard implements LauncherGuardInterface {
    @Override
    public String getName() {
        return "java";
    }

    @Override
    public Path getJavaBinPath() {
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE)
            return IOHelper.resolveJavaBin(ClientLauncher.getJavaBinPath());
        else
            return IOHelper.resolveJavaBin(Paths.get(System.getProperty("java.home")));
    }

    @Override
    public void init(boolean clientInstance) {

    }
}
