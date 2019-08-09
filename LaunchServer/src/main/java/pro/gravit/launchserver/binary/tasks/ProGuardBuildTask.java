package pro.gravit.launchserver.binary.tasks;

import java.io.IOException;
import java.nio.file.Path;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;
import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.ParseException;
import proguard.ProGuard;

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
