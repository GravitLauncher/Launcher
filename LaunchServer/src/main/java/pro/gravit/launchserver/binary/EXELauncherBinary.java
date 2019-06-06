package pro.gravit.launchserver.binary;

import java.io.IOException;
import java.nio.file.Files;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class EXELauncherBinary extends LauncherBinary {

    public EXELauncherBinary(LaunchServer server) {
        super(server, LauncherBinary.resolve(server, ".exe"));
    }

    @Override
    public void build() throws IOException {
        if (IOHelper.isFile(syncBinaryFile)) {
            LogHelper.subWarning("Deleting obsolete launcher EXE binary file");
            Files.delete(syncBinaryFile);
        }
    }

}
