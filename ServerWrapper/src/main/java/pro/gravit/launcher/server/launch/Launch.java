package pro.gravit.launcher.server.launch;

import pro.gravit.launcher.server.ServerWrapper;

public interface Launch {
    void run(ServerWrapper.Config config, String[] args) throws Throwable;
}
