package ru.gravit.launchserver.binary.tasks;

import java.io.IOException;
import java.nio.file.Path;

import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.ParseException;
import proguard.ProGuard;
import ru.gravit.launchserver.LaunchServer;

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
