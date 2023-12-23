package pro.gravit.launcher.runtime.client.events;

import pro.gravit.launcher.runtime.gui.RuntimeProvider;
import pro.gravit.launcher.base.modules.LauncherModule;

public class ClientGuiPhase extends LauncherModule.Event {
    public final RuntimeProvider runtimeProvider;

    public ClientGuiPhase(RuntimeProvider runtimeProvider) {
        this.runtimeProvider = runtimeProvider;
    }
}
