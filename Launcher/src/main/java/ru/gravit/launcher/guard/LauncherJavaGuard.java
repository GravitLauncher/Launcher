package ru.gravit.launcher.guard;

import ru.gravit.launcher.client.ClientLauncher;
import ru.gravit.launcher.client.ClientLauncherContext;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

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
    public int getClientJVMBits() {
        return JVMHelper.OS_BITS;
    }

    @Override
    public void init(boolean clientInstance) {

    }

    @Override
    public void addCustomParams(ClientLauncherContext context) {
        Collections.addAll(context.args, "-cp");
        Collections.addAll(context.args, context.pathLauncher);
    }

    @Override
    public void addCustomEnv(ClientLauncherContext context) {

    }

    @Override
    public void setProtectToken(String token) {
        //Skip
    }
}
