package pro.gravit.launchserver.binary;

import pro.gravit.launcher.core.api.features.CoreFeatureAPI;
import pro.gravit.launchserver.LaunchServer;

import java.io.IOException;

public class EXELauncherBinary extends LauncherBinary {

    public EXELauncherBinary(LaunchServer server) {
        super(server);
    }

    @Override
    public CoreFeatureAPI.UpdateVariant getVariant() {
        return CoreFeatureAPI.UpdateVariant.EXE_WINDOWS_X86_64;
    }

    @Override
    public PipelineContext build() throws IOException {
        return new PipelineContext(server);
    }

}
