package pro.gravit.launchserver.binary;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public class SimpleEXELauncherBinary extends LauncherBinary {
    public Path exeTemplate;
    protected SimpleEXELauncherBinary(LaunchServer server) {
        super(server, LauncherBinary.resolve(server, ".exe"));
        exeTemplate = server.dir.resolve("SimpleTemplate.exe");
    }

    @Override
    public void build() throws IOException {
        try(OutputStream output = IOHelper.newOutput(syncBinaryFile))
        {
            IOHelper.transfer(exeTemplate, output);
            IOHelper.transfer(server.launcherBinary.syncBinaryFile, output);
        }
    }
}
