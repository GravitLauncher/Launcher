package pro.gravit.launcher.client;

import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.impl.SimpleModuleManager;

import java.io.IOException;
import java.nio.file.Path;

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
}
