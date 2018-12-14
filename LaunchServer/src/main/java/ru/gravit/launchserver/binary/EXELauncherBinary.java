package ru.gravit.launchserver.binary;

import java.io.IOException;
import java.nio.file.Files;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

public class EXELauncherBinary extends LauncherBinary {

    public EXELauncherBinary(LaunchServer server) {
        super(server, server.dir.resolve(server.config.binaryName + ".exe"));
    }

    @Override
    public void build() throws IOException {
        if (IOHelper.isFile(binaryFile)) {
            LogHelper.subWarning("Deleting obsolete launcher EXE binary file");
            Files.delete(binaryFile);
        }
    }

}
