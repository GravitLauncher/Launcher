package pro.gravit.launcher.client.runtime.client.events;

import pro.gravit.launcher.client.runtime.gui.RuntimeProvider;
import pro.gravit.launcher.modules.LauncherModule;

public class ClientGuiPhase extends LauncherModule.Event {
    public final RuntimeProvider runtimeProvider;

    public ClientGuiPhase(RuntimeProvider runtimeProvider) {
        this.runtimeProvider = runtimeProvider;
    }
}
