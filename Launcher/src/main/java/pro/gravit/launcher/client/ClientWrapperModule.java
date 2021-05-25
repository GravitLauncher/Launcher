package pro.gravit.launcher.client;

import pro.gravit.launcher.ClientLauncherWrapper;

public interface ClientWrapperModule {
    void wrapperPhase(ClientLauncherWrapper.ClientLauncherWrapperContext context);
}
