package pro.gravit.launchserver.modules.events;

import pro.gravit.launcher.modules.events.InitPhase;
import pro.gravit.launchserver.LaunchServer;

public class LaunchServerPostInitPhase extends InitPhase {
    public final LaunchServer server;

    public LaunchServerPostInitPhase(LaunchServer server) {
        this.server = server;
    }
}
