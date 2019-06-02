package pro.gravit.launcher.test.utils;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.LauncherBinary;
import pro.gravit.utils.helper.IOHelper;

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
