package pro.gravit.launcher.server.launch;

import pro.gravit.launcher.server.ServerWrapper;

public class ModuleLaunch implements Launch {
    @Override
    public void run(ServerWrapper.Config config, String[] args) throws Throwable {
        throw new UnsupportedOperationException("Module system not supported");
    }
}
