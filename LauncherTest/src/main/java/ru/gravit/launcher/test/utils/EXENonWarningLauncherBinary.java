package ru.gravit.launcher.test.utils;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.binary.LauncherBinary;
import ru.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.nio.file.Files;

public class EXENonWarningLauncherBinary extends LauncherBinary {

    public EXENonWarningLauncherBinary(LaunchServer server) {
        super(server, server.dir.resolve(server.config.binaryName + ".exe"));
    }

    @Override
    public void build() throws IOException {
        if (IOHelper.isFile(syncBinaryFile)) {
            Files.delete(syncBinaryFile);
        }
    }

}
