package pro.gravit.launcher;

import pro.gravit.launcher.ClientLauncherWrapper;

public interface ClientWrapperModule {
    void wrapperPhase(ClientLauncherWrapper.ClientLauncherWrapperContext context);
}
