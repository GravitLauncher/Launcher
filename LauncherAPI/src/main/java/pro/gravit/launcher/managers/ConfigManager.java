package pro.gravit.launcher.managers;

import pro.gravit.launcher.config.JsonConfigurable;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;

@SuppressWarnings("rawtypes")
public class ConfigManager {
    private final HashMap<String, JsonConfigurable> CONFIGURABLE = new HashMap<>();

    public void registerConfigurable(String name, JsonConfigurable reconfigurable) {
        VerifyHelper.putIfAbsent(CONFIGURABLE, name.toLowerCase(), Objects.requireNonNull(reconfigurable, "adapter"),
                String.format("Reloadable has been already registered: '%s'", name));
    }

    public void printConfigurables() {
        LogHelper.info("Print configurables");
        CONFIGURABLE.forEach((k, v) -> LogHelper.subInfo(k));
        LogHelper.info("Found %d configurables", CONFIGURABLE.size());
    }

    public void save(String name) throws IOException {
        CONFIGURABLE.get(name).saveConfig();
    }

    public void load(String name) throws IOException {
        CONFIGURABLE.get(name).loadConfig();
    }

    public void save(String name, Path path) throws IOException {
        CONFIGURABLE.get(name).saveConfig(path);
    }

    public void reset(String name) throws IOException {
        CONFIGURABLE.get(name).resetConfig();
    }

    public void load(String name, Path path) throws IOException {
        CONFIGURABLE.get(name).loadConfig(path);
    }

    public void reset(String name, Path path) throws IOException {
        CONFIGURABLE.get(name).resetConfig(path);
    }
}
