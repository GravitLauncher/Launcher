package pro.gravit.launcher.runtime.client.events;

import pro.gravit.launcher.runtime.LauncherEngine;
import pro.gravit.launcher.base.modules.events.InitPhase;

public class ClientEngineInitPhase extends InitPhase {
    public final LauncherEngine engine;

    public ClientEngineInitPhase(LauncherEngine engine) {
        this.engine = engine;
    }
}
