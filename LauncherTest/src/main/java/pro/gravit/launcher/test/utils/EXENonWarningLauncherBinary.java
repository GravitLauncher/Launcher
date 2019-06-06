package pro.gravit.launcher.test.utils;

import java.io.IOException;
import java.nio.file.Files;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.LauncherBinary;
import pro.gravit.utils.helper.IOHelper;

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
