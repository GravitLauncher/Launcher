package ru.gravit.launchserver.binary.tasks;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.UnpackHelper;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public class UnpackBuildTask implements LauncherBuildTask {
    public static LaunchServer server = LaunchServer.server;
    @Override
    public String getName() {
        return "unpack clean";
    }

    @Override
    public int priority() {
        return -1;
    }

    @Override
    public Path process(Path inputFile) throws IOException {
        Path result = server.dir.resolve(server.config.binaryName + "-clean.jar");
        URL url = IOHelper.getResourceURL("Launcher.jar");
        UnpackHelper.unpack(url, result);
        return result;
    }
}
