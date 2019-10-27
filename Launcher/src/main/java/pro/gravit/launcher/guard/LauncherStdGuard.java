package pro.gravit.launcher.guard;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.client.ClientLauncher;
import pro.gravit.launcher.client.ClientLauncherContext;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.UnpackHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

//Стандартный интерфейс для всех AntiInject
public class LauncherStdGuard implements LauncherGuardInterface {
    public String protectToken;
    public Path javaBinPath;

    @Override
    public String getName() {
        return "stdguard";
    }

    @Override
    public Path getJavaBinPath() {
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) {
            javaBinPath = ClientLauncher.getJavaBinPath();
            String projectName = Launcher.getConfig().projectName;
            String wrapperUnpackName = (javaBinPath == null ? JVMHelper.JVM_BITS : JVMHelper.OS_BITS) == 64 ? projectName.concat("64.exe") : projectName.concat("32.exe");
            return DirBridge.getGuardDir().resolve(wrapperUnpackName);
        } else
            return IOHelper.resolveJavaBin(Paths.get(System.getProperty("java.home")));
    }

    @Override
    public int getClientJVMBits() {
        //При использовании GravitGuard без своей джавы
        //Если при запуске лаунчера используется 32 бит джава, а ОС 64бит
        //То в окне настроек будет отображаться >1.5Гб доступной памяти
        //Однако при выставлении >1.5Гб JVM x32 работать откажеться
        return JVMHelper.OS_BITS;
    }

    @Override
    public void init(boolean clientInstance) {
        try {
            String projectName = Launcher.getConfig().projectName;
            UnpackHelper.unpack(Launcher.getResourceURL("wrapper64.exe", "guard"), DirBridge.getGuardDir().resolve(projectName.concat("64.exe")));
            UnpackHelper.unpack(Launcher.getResourceURL("AntiInject64.dll", "guard"), DirBridge.getGuardDir().resolve("AntiInject64.dll"));

            UnpackHelper.unpack(Launcher.getResourceURL("wrapper32.exe", "guard"), DirBridge.getGuardDir().resolve(projectName.concat("32.exe")));
            UnpackHelper.unpack(Launcher.getResourceURL("AntiInject32.dll", "guard"), DirBridge.getGuardDir().resolve("AntiInject32.dll"));
        } catch (IOException e) {
            throw new SecurityException(e);
        }
    }

    @Override
    public void addCustomParams(ClientLauncherContext context) {
        Collections.addAll(context.args, "-Djava.class.path=".concat(context.pathLauncher));
    }

    @Override
    public void addCustomEnv(ClientLauncherContext context) {
        Map<String, String> env = context.builder.environment();
        if (javaBinPath == null)
            env.put("JAVA_HOME", System.getProperty("java.home"));
        else
            env.put("JAVA_HOME", javaBinPath.toAbsolutePath().toString());
        LauncherConfig config = Launcher.getConfig();
        env.put("GUARD_USERNAME", context.playerProfile.username);
        env.put("GUARD_PROJECTNAME", config.projectName);
        if (protectToken != null)
            env.put("GUARD_TOKEN", protectToken);
    }

    @Override
    public void setProtectToken(String token) {
        protectToken = token;
    }
}
