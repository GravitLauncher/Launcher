package ru.gravit.launchserver.binary.tasks;

import java.io.IOException;
import java.nio.file.Path;

import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.ParseException;
import proguard.ProGuard;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

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
    	if (server.config.enabledProGuard) {
    		Path outputJar = server.launcherBinary.nextLowerPath(this);
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
    		return outputJar;
    	} else {
    		Path outputJar = server.launcherBinary.nextPath("non-obf");
    		IOHelper.copy(inputFile, outputJar);
    		return outputJar;
    	}
    }

    @Override
    public boolean allowDelete() {
        return true;
    }
}
