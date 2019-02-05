package ru.gravit.launcher.guard;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.client.DirBridge;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.UnpackHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LauncherWrapperGuard implements LauncherGuardInterface {
    @Override
    public String getName() {
        return "wrapper";
    }

    @Override
    public Path getJavaBinPath() {
        if(JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE)
            return DirBridge.getGuardDir().resolve(JVMHelper.JVM_BITS == 64 ? "wrapper64.exe" : "wrapper32.exe");
        else
            return IOHelper.resolveJavaBin(Paths.get(System.getProperty("java.home")));
    }

    @Override
    public void init(boolean clientInstance) {
        try {
            String wrapperName = JVMHelper.JVM_BITS == 64 ? "wrapper64.exe" : "wrapper32.exe";
            String antiInjectName = JVMHelper.JVM_BITS == 64 ? "AntiInject64.dll" : "AntiInject32.dll";
            UnpackHelper.unpack(Launcher.getResourceURL(wrapperName, "guard"),DirBridge.getGuardDir().resolve(wrapperName));
            UnpackHelper.unpack(Launcher.getResourceURL(antiInjectName, "guard"),DirBridge.getGuardDir().resolve(antiInjectName));
        } catch (IOException e) {
            throw new SecurityException(e);
        }
    }
}
