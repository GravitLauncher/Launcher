package pro.gravit.launcher.client.events.client;

import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.utils.launch.ClassLoaderControl;
import pro.gravit.utils.launch.Launch;

public class ClientProcessClassLoaderEvent extends LauncherModule.Event {
    public final Launch launch;
    public final ClassLoaderControl classLoaderControl;
    public final ClientProfile profile;

    public ClientProcessClassLoaderEvent(Launch launch, ClassLoaderControl classLoaderControl, ClientProfile profile) {
        this.launch = launch;
        this.classLoaderControl = classLoaderControl;
        this.profile = profile;
    }
}
