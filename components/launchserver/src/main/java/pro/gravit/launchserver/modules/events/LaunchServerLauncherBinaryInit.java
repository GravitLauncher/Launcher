package pro.gravit.launchserver.modules.events;

import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.core.api.features.CoreFeatureAPI;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.LauncherBinary;

import java.util.Map;

public class LaunchServerLauncherBinaryInit extends LauncherModule.Event {
    public final LaunchServer server;
    public final Map<CoreFeatureAPI.UpdateVariant, LauncherBinary> binary;

    public LaunchServerLauncherBinaryInit(LaunchServer server, Map<CoreFeatureAPI.UpdateVariant, LauncherBinary> binary) {
        this.server = server;
        this.binary = binary;
    }
}
