package ru.gravit.launcher.guard;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.bridge.GravitGuardBridge;
import ru.gravit.launcher.client.ClientLauncher;
import ru.gravit.launcher.client.ClientLauncherContext;
import ru.gravit.launcher.client.DirBridge;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.UnpackHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

//Используется для всех типов защит, совместимых с новым GravitGuard API
public class LauncherGravitGuard implements LauncherGuardInterface {
    public String protectToken;
    public Path javaBinPath;

    @Override
    public String getName() {
        return "wrapper";
    }

    @Override
    public Path getJavaBinPath() {
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) {
            String projectName = Launcher.getConfig().projectname;
            String wrapperUnpackName = JVMHelper.JVM_BITS == 64 ? projectName.concat("64.exe") : projectName.concat("32.exe");
            return DirBridge.getGuardDir().resolve(wrapperUnpackName);
        } else if(ClientLauncher.getJavaBinPath() != null)
        {
            javaBinPath = ClientLauncher.getJavaBinPath();
            String projectName = Launcher.getConfig().projectname;
            String wrapperUnpackName = JVMHelper.JVM_BITS == 64 ? projectName.concat("64.exe") : projectName.concat("32.exe");
            return DirBridge.getGuardDir().resolve(wrapperUnpackName);
        }
        else
            return IOHelper.resolveJavaBin(Paths.get(System.getProperty("java.home")));
    }

    @Override
    public int getClientJVMBits() {
        return JVMHelper.JVM_BITS;
    }

    @Override
    public void init(boolean clientInstance) {
        try {
            String wrapperName = JVMHelper.JVM_BITS == 64 ? "wrapper64.exe" : "wrapper32.exe";
            String projectName = Launcher.getConfig().projectname;
            String wrapperUnpackName = JVMHelper.JVM_BITS == 64 ? projectName.concat("64.exe") : projectName.concat("32.exe");
            String antiInjectName = JVMHelper.JVM_BITS == 64 ? "AntiInject64.dll" : "AntiInject32.dll";
            UnpackHelper.unpack(Launcher.getResourceURL(wrapperName, "guard"), DirBridge.getGuardDir().resolve(wrapperUnpackName));
            UnpackHelper.unpack(Launcher.getResourceURL(antiInjectName, "guard"), DirBridge.getGuardDir().resolve(antiInjectName));
        } catch (IOException e) {
            throw new SecurityException(e);
        }
        if(clientInstance) GravitGuardBridge.callGuard();
    }

    @Override
    public void addCustomParams(ClientLauncherContext context) {
        Collections.addAll(context.args, "-Djava.class.path=".concat(context.pathLauncher));
    }

    @Override
    public void addCustomEnv(ClientLauncherContext context) {
        Map<String, String> env = context.builder.environment();
        if(javaBinPath == null)
            env.put("JAVA_HOME", System.getProperty("java.home"));
        else
            env.put("JAVA_HOME", javaBinPath.toAbsolutePath().toString());
        LauncherConfig config = Launcher.getConfig();
        env.put("GUARD_BRIDGE", GravitGuardBridge.class.getName());
        env.put("GUARD_USERNAME", context.playerProfile.username);
        env.put("GUARD_PUBLICKEY", config.publicKey.getModulus().toString(16));
        env.put("GUARD_PROJECTNAME", config.projectname);
        if (protectToken != null)
            env.put("GUARD_TOKEN", protectToken);
        if (config.guardLicenseName != null)
            env.put("GUARD_LICENSE_NAME", config.guardLicenseName);
        if (config.guardLicenseKey != null) {
            env.put("GUARD_LICENSE_KEY", config.guardLicenseKey);
        }
    }

    @Override
    public void setProtectToken(String token) {
        protectToken = token;
    }
}
