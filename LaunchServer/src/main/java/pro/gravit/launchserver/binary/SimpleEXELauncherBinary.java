package pro.gravit.launchserver.binary;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public class SimpleEXELauncherBinary extends LauncherBinary {
    public final Path exeTemplate;

    public SimpleEXELauncherBinary(LaunchServer server) {
        super(server, LauncherBinary.resolve(server, ".exe"));
        exeTemplate = server.dir.resolve("SimpleTemplate.exe");
    }

    @Override
    public void build() throws IOException {
        if (!IOHelper.isFile(exeTemplate)) {
            LogHelper.warning("[SimpleEXEBinary] File %s not found. %s not created", exeTemplate.toString(), syncBinaryFile.toString());
            return;
        }
        try (OutputStream output = IOHelper.newOutput(syncBinaryFile)) {
            IOHelper.transfer(exeTemplate, output);
            IOHelper.transfer(server.launcherBinary.syncBinaryFile, output);
        }
    }
}
