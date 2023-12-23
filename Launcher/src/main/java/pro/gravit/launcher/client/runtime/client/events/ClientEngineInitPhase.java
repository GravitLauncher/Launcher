package pro.gravit.launcher.client.runtime.client.events;

import pro.gravit.launcher.client.runtime.LauncherEngine;
import pro.gravit.launcher.modules.events.InitPhase;

public class ClientEngineInitPhase extends InitPhase {
    public final LauncherEngine engine;

    public ClientEngineInitPhase(LauncherEngine engine) {
        this.engine = engine;
    }
}
