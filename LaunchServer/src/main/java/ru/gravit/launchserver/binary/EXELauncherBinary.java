package ru.gravit.launchserver.binary;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.Files;

public class EXELauncherBinary extends LauncherBinary {

    public EXELauncherBinary(LaunchServer server) {
        super(server, server.dir.resolve(server.config.binaryName + ".exe"));
    }

    @Override
    public void build() throws IOException {
        if (IOHelper.isFile(syncBinaryFile)) {
            LogHelper.subWarning("Deleting obsolete launcher EXE binary file");
            Files.delete(syncBinaryFile);
        }
    }

}
