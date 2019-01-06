package ru.gravit.launchserver.binary.tasks;

import ru.gravit.launcher.Launcher;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.UnpackHelper;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public class UnpackBuildTask implements LauncherBuildTask {
    public static final Path runtimeDir = LaunchServer.server.dir.resolve(Launcher.RUNTIME_DIR);
    public static final Path guardDir = LaunchServer.server.dir.resolve(Launcher.GUARD_DIR);
    public static LaunchServer server = LaunchServer.server;

    @Override
    public String getName() {
        return "UnpackFromResources";
    }

    @Override
    public Path process(Path inputFile) throws IOException {
        Path result = server.dir.resolve(server.config.binaryName + "-clean.jar");
        URL url = IOHelper.getResourceURL("Launcher.jar");
        UnpackHelper.unpack(url, result);
        return result;
    }

    @Override
    public boolean allowDelete() {
        return false;
    }

    public void tryUnpack() throws IOException {
        LogHelper.info("Unpacking launcher native guard files and runtime");
        UnpackHelper.unpackZipNoCheck("guard.zip", guardDir);
        UnpackHelper.unpackZipNoCheck("runtime.zip", runtimeDir);
    }
}
