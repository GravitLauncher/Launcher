package pro.gravit.launcher.client.events.client;

import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.profiles.ClientProfile;

public class ClientProcessClassLoaderEvent extends LauncherModule.Event {
    public final LauncherEngine clientInstance;
    public final ClassLoader clientClassLoader;
    public final ClientProfile profile;

    public ClientProcessClassLoaderEvent(LauncherEngine clientInstance, ClassLoader clientClassLoader, ClientProfile profile) {
        this.clientInstance = clientInstance;
        this.clientClassLoader = clientClassLoader;
        this.profile = profile;
    }
}
