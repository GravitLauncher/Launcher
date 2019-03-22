package ru.gravit.launcher.guard;

import ru.gravit.launcher.client.ClientLauncherContext;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

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
    public int getClientJVMBits() {
        return JVMHelper.JVM_BITS;
    }

    @Override
    public void init(boolean clientInstance) {
        LogHelper.warning("Using noGuard interface");
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
