package pro.gravit.launcher.server.launch;

import pro.gravit.launcher.server.ServerWrapper;

public class ModuleLaunch implements Launch {

    @Override
    public void run(String mainClass, ServerWrapper.Config config, String[] args) {
        throw new UnsupportedOperationException("Module system not supported");
    }
}
