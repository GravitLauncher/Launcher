package pro.gravit.launcher.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.impl.SimpleModuleManager;

public class ClientModuleManager extends SimpleModuleManager {
    public ClientModuleManager() {
        super(null, null);
    }

    @Override
    public void autoload() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void autoload(Path dir) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public LauncherModule loadModule(Path file) throws IOException {
        throw new UnsupportedOperationException();
    }
    public void callWrapper(ProcessBuilder processBuilder, Collection<String> jvmArgs)
    {
        for(LauncherModule module : modules)
        {
            if(module instanceof ClientWrapperModule)
            {
                ((ClientWrapperModule) module).wrapperPhase(processBuilder, jvmArgs);
            }
        }
    }
}
