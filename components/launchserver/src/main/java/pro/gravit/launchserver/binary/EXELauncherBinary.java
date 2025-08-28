package pro.gravit.launchserver.binary;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.updates.UpdatesProvider;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.nio.file.Files;

public class EXELauncherBinary extends LauncherBinary {

    public EXELauncherBinary(LaunchServer server) {
        super(server);
    }

    @Override
    public UpdatesProvider.UpdateVariant getVariant() {
        return UpdatesProvider.UpdateVariant.EXE;
    }

    @Override
    public PipelineContext build() throws IOException {
        return new PipelineContext(server);
    }

}
