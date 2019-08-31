package pro.gravit.launchserver.modules.events;

import pro.gravit.launcher.modules.events.PostInitPhase;
import pro.gravit.launchserver.LaunchServer;

public class LaunchServerPostInitPhase extends PostInitPhase {
    public final LaunchServer server;

    public LaunchServerPostInitPhase(LaunchServer server) {
        this.server = server;
    }
}
