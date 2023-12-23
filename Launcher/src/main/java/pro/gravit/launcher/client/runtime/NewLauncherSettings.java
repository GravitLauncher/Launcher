package pro.gravit.launcher.client.runtime;

import pro.gravit.launcher.client.runtime.client.UserSettings;
import pro.gravit.launcher.core.LauncherNetworkAPI;

import java.util.HashMap;
import java.util.Map;

public class NewLauncherSettings {
    @LauncherNetworkAPI
    public Map<String, UserSettings> userSettings = new HashMap<>();
    @LauncherNetworkAPI
    public String consoleUnlockKey;
}
