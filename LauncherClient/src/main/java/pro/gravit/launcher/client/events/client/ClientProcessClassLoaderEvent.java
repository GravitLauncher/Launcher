package pro.gravit.launcher.client.events.client;

import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.profiles.ClientProfile;

public class ClientProcessClassLoaderEvent extends LauncherModule.Event {
    public final ClassLoader clientClassLoader;
    public final ClientProfile profile;

    public ClientProcessClassLoaderEvent(ClassLoader clientClassLoader, ClientProfile profile) {
        this.clientClassLoader = clientClassLoader;
        this.profile = profile;
    }
}
