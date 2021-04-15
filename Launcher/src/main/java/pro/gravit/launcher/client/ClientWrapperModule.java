package pro.gravit.launcher.client;

import pro.gravit.launcher.ClientLauncherWrapper;

import java.util.Collection;

public interface ClientWrapperModule {
    void wrapperPhase(ClientLauncherWrapper.ClientLauncherWrapperContext context);
}
