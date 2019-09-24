package pro.gravit.launcher.config;

import com.google.gson.Gson;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherAPI;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;

public interface JsonConfigurableInterface<T> {
    @LauncherAPI
    default void saveConfig() throws IOException {
        saveConfig(getPath());
    }
    @LauncherAPI
    default void loadConfig() throws IOException {
        loadConfig(getPath());
    }
    @LauncherAPI
    default void saveConfig(Gson gson, Path configPath) throws IOException {
        try (BufferedWriter writer = IOHelper.newWriter(configPath)) {
            gson.toJson(getConfig(), getType(), writer);
        }
    }
    @LauncherAPI
    default void loadConfig(Gson gson, Path configPath) throws IOException {
        if (generateConfigIfNotExists(configPath)) return;
        try (BufferedReader reader = IOHelper.newReader(configPath)) {
            setConfig(gson.fromJson(reader, getType()));
        } catch (Exception e)
        {
            LogHelper.error(e);
            resetConfig(configPath);
        }
    }
    @LauncherAPI
    default void saveConfig(Path configPath) throws IOException {
        saveConfig(Launcher.gsonManager.configGson, configPath);
    }
    @LauncherAPI
    default void loadConfig(Path configPath) throws IOException {
        loadConfig(Launcher.gsonManager.configGson, configPath);
    }
    @LauncherAPI
    default void resetConfig() throws IOException {
        setConfig(getDefaultConfig());
        saveConfig();
    }
    @LauncherAPI
    default void resetConfig(Path newPath) throws IOException {
        setConfig(getDefaultConfig());
        saveConfig(newPath);
    }
    @LauncherAPI
    default boolean generateConfigIfNotExists(Path path) throws IOException {
        if (IOHelper.isFile(path))
            return false;
        resetConfig(path);
        return true;
    }
    @LauncherAPI
    default boolean generateConfigIfNotExists() throws IOException {
        if (IOHelper.isFile(getPath()))
            return false;
        resetConfig();
        return true;
    }
    @LauncherAPI
    T getConfig();
    @LauncherAPI
    T getDefaultConfig();
    @LauncherAPI
    void setConfig(T config);
    @LauncherAPI
    Path getPath();

    Type getType();

}
