package pro.gravit.launchserver.modules.events;

import pro.gravit.launcher.modules.events.InitPhase;
import pro.gravit.launchserver.LaunchServer;

public class LaunchServerInitPhase extends InitPhase {
    public final LaunchServer server;

    public LaunchServerInitPhase(LaunchServer server) {
        this.server = server;
    }
}
