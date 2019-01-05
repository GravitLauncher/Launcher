package ru.gravit.launchserver.binary.tasks;

import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.ParseException;
import proguard.ProGuard;
import ru.gravit.launchserver.LaunchServer;

import java.io.IOException;
import java.nio.file.Path;

public class ProGuardBuildTask implements LauncherBuildTask {
    public static LaunchServer server = LaunchServer.server;
    @Override
    public String getName() {
        return "proguard";
    }

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public Path process(Path inputFile) throws IOException {
        Configuration proguard_cfg = new Configuration();
        server.proguardConf.buildConfig(inputFile);
        ConfigurationParser parser = new ConfigurationParser(server.proguardConf.confStrs.toArray(new String[0]),
                server.proguardConf.proguard.toFile(), System.getProperties());
        try {
            parser.parse(proguard_cfg);
            ProGuard proGuard = new ProGuard(proguard_cfg);
            proGuard.execute();
        } catch (ParseException e1) {
            e1.printStackTrace();
        }
        return server.proguardConf.outputJar;
    }

    @Override
    public boolean allowDelete() {
        return true;
    }
}
