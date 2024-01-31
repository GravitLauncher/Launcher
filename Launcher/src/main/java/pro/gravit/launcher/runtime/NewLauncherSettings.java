package pro.gravit.launcher.runtime;

import pro.gravit.launcher.runtime.client.UserSettings;
import pro.gravit.launcher.core.LauncherNetworkAPI;

import java.util.HashMap;
import java.util.Map;

public class NewLauncherSettings {
    @LauncherNetworkAPI
    public Map<String, UserSettings> userSettings = new HashMap<>();
    @LauncherNetworkAPI
    public String consoleUnlockKey;
}
