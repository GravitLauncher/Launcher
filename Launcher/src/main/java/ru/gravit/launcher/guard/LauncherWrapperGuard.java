package ru.gravit.launcher.guard;

import ru.gravit.launcher.client.DirBridge;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;
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
            UnpackHelper.unpack(IOHelper.getResourceURL(JVMHelper.JVM_BITS == 64 ? "guard/wrapper64.exe" : "guard/wrapper32.exe"),DirBridge.getGuardDir());
        } catch (IOException e) {
            throw new SecurityException(e);
        }
    }
}
