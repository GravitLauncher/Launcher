package pro.gravit.launcher.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherAPI;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public abstract class JsonConfigurable<T> {
    private Type type;
    protected Path configPath;

    @LauncherAPI
    public void saveConfig() throws IOException {
        saveConfig(configPath);
    }

    @LauncherAPI
    public void loadConfig() throws IOException {
        loadConfig(configPath);
    }

    @LauncherAPI
    public JsonConfigurable(Type type, Path configPath) {
        this.type = type;
        this.configPath = configPath;
    }

    @LauncherAPI
    public void saveConfig(Path configPath) throws IOException {
        try (BufferedWriter writer = IOHelper.newWriter(configPath)) {
            Launcher.gsonManager.configGson.toJson(getConfig(), type, writer);
        }
    }

    @LauncherAPI
    public void loadConfig(Path configPath) throws IOException {
        if (generateConfigIfNotExists(configPath)) return;
        try (BufferedReader reader = IOHelper.newReader(configPath)) {
            setConfig(Launcher.gsonManager.configGson.fromJson(reader, type));
        } catch (Exception e)
        {
            LogHelper.error(e);
            resetConfig(configPath);
        }
    }

    @LauncherAPI
    public void resetConfig() throws IOException {
        setConfig(getDefaultConfig());
        saveConfig();
    }

    @LauncherAPI
    public void resetConfig(Path newPath) throws IOException {
        setConfig(getDefaultConfig());
        saveConfig(newPath);
    }

    @LauncherAPI
    public boolean generateConfigIfNotExists(Path path) throws IOException {
        if (IOHelper.isFile(path))
            return false;
        resetConfig(path);
        return true;
    }

    @LauncherAPI
    public boolean generateConfigIfNotExists() throws IOException {
        if (IOHelper.isFile(configPath))
            return false;
        resetConfig();
        return true;
    }

    protected void setType(Type type) {
        this.type = type;
    }

    @LauncherAPI
    public abstract T getConfig();

    @LauncherAPI
    public abstract T getDefaultConfig();

    @LauncherAPI
    public abstract void setConfig(T config);
}
