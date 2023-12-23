package pro.gravit.launcher.client.runtime.client.events;

import pro.gravit.launcher.client.runtime.gui.RuntimeProvider;
import pro.gravit.launcher.modules.LauncherModule;

public class ClientPreGuiPhase extends LauncherModule.Event {
    public RuntimeProvider runtimeProvider;

    public ClientPreGuiPhase(RuntimeProvider runtimeProvider) {
        this.runtimeProvider = runtimeProvider;
    }
}
