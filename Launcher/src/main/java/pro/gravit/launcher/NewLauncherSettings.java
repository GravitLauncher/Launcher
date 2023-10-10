package pro.gravit.launcher;

import pro.gravit.launcher.client.UserSettings;
import pro.gravit.launcher.hasher.HashedDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewLauncherSettings {
    @LauncherNetworkAPI
    public Map<String, UserSettings> userSettings = new HashMap<>();
    @LauncherNetworkAPI
    public String consoleUnlockKey;
}
