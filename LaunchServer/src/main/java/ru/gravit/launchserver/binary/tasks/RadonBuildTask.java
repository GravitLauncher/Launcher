package ru.gravit.launchserver.binary.tasks;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.UnpackHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

import me.itzsomebody.radon.Radon;
import me.itzsomebody.radon.SessionInfo;
import me.itzsomebody.radon.config.ConfigurationParser;

public class RadonBuildTask implements LauncherBuildTask {
    private final LaunchServer srv;
    public final Path config;

    public RadonBuildTask(LaunchServer srv) {
        this.srv = srv;
        config = this.srv.dir.resolve("radon.yml");
    }

    @Override
    public String getName() {
        return "Radon";
    }

    @Override
    public Path process(Path inputFile) throws IOException {
    	if (!IOHelper.isFile(config)) UnpackHelper.unpack(IOHelper.getResourceURL("ru/gravit/launchserver/defaults/radon.cfg"), config);
        Path outputFile = srv.launcherBinary.nextLowerPath(this);
        ConfigurationParser p = new ConfigurationParser(IOHelper.newInput(config));
        SessionInfo info = p.createSessionFromConfig();
        info.setInput(inputFile.toFile());
        info.setOutput(outputFile.toFile());
        info.setLibraries(srv.launcherBinary.coreLibs.stream().map(e -> e.toFile()).collect(Collectors.toList()));
        Radon r = new Radon(info);
        r.run();
        return outputFile;
    }

    @Override
    public boolean allowDelete() {
        return true;
    }
}
