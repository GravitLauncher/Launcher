package pro.gravit.launcher.client.events;

import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.modules.events.InitPhase;

public class ClientEngineInitPhase extends InitPhase {
    public final LauncherEngine engine;

    public ClientEngineInitPhase(LauncherEngine engine) {
        this.engine = engine;
    }
}
