package pro.gravit.launcher.runtime.client.events;

import pro.gravit.launcher.runtime.gui.RuntimeProvider;
import pro.gravit.launcher.base.modules.LauncherModule;

public class ClientPreGuiPhase extends LauncherModule.Event {
    public RuntimeProvider runtimeProvider;

    public ClientPreGuiPhase(RuntimeProvider runtimeProvider) {
        this.runtimeProvider = runtimeProvider;
    }
}
