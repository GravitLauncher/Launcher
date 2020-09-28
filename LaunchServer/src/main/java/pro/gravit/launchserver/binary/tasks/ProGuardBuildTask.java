package pro.gravit.launchserver.binary.tasks;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;
import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.ParseException;
import proguard.ProGuard;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProGuardBuildTask implements LauncherBuildTask {
    private final LaunchServer server;

    public ProGuardBuildTask(LaunchServer server) {
        this.server = server;
    }

    @Override
    public String getName() {
        return "ProGuard";
    }

    @Override
    public Path process(Path inputFile) throws IOException {
        Path outputJar = server.launcherBinary.nextLowerPath(this);
        if (server.config.launcher.enabledProGuard) {
            Configuration proguard_cfg = new Configuration();
            ConfigurationParser parser = new ConfigurationParser(server.proguardConf.buildConfig(inputFile, outputJar),
                    server.proguardConf.proguard.toFile(), System.getProperties());
            if (JVMHelper.JVM_VERSION >= 9) {
                Path javaJModsPath = Paths.get(System.getProperty("java.home")).resolve("jmods");
                if (!IOHelper.exists(javaJModsPath)) {
                    LogHelper.warning("Directory %s not found. It is not good", javaJModsPath);
                } else {
                    //Find javaFX libraries
                    if (!IOHelper.exists(javaJModsPath.resolve("javafx.base.jmod")))
                        LogHelper.error("javafx.base.jmod not found. Launcher can be assembled incorrectly. Maybe you need to install OpenJFX?");
                    if (!IOHelper.exists(javaJModsPath.resolve("javafx.graphics.jmod")))
                        LogHelper.error("javafx.graphics.jmod not found. Launcher can be assembled incorrectly. Maybe you need to install OpenJFX?");
                    if (!IOHelper.exists(javaJModsPath.resolve("javafx.controls.jmod")))
                        LogHelper.error("javafx.controls.jmod not found. Launcher can be assembled incorrectly. Maybe you need to install OpenJFX?");
                }
            }
            try {
                parser.parse(proguard_cfg);
                ProGuard proGuard = new ProGuard(proguard_cfg);
                proGuard.execute();
            } catch (ParseException e) {
                LogHelper.error(e);
            }
        } else
            IOHelper.copy(inputFile, outputJar);
        return outputJar;
    }

    @Override
    public boolean allowDelete() {
        return true;
    }
}
