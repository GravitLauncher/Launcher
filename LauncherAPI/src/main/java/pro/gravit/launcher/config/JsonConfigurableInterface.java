package pro.gravit.launcher.config;

import com.google.gson.Gson;
import pro.gravit.launcher.Launcher;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;

public interface JsonConfigurableInterface<T> {

    default void saveConfig() throws IOException {
        saveConfig(getPath());
    }


    default void loadConfig() throws IOException {
        loadConfig(getPath());
    }


    default void saveConfig(Gson gson, Path configPath) throws IOException {
        try (BufferedWriter writer = IOHelper.newWriter(configPath)) {
            gson.toJson(getConfig(), getType(), writer);
        }
    }

    default String toJsonString(Gson gson) {
        return gson.toJson(getConfig(), getType());
    }

    default String toJsonString() {
        return toJsonString(Launcher.gsonManager.configGson);
    }


    default void loadConfig(Gson gson, Path configPath) throws IOException {
        if (generateConfigIfNotExists(configPath)) return;
        try (BufferedReader reader = IOHelper.newReader(configPath)) {
            setConfig(gson.fromJson(reader, getType()));
        } catch (Exception e) {
            LogHelper.error(e);
            resetConfig(configPath);
        }
    }


    default void saveConfig(Path configPath) throws IOException {
        saveConfig(Launcher.gsonManager.configGson, configPath);
    }


    default void loadConfig(Path configPath) throws IOException {
        loadConfig(Launcher.gsonManager.configGson, configPath);
    }


    default void resetConfig() throws IOException {
        setConfig(getDefaultConfig());
        saveConfig();
    }


    default void resetConfig(Path newPath) throws IOException {
        setConfig(getDefaultConfig());
        saveConfig(newPath);
    }


    default boolean generateConfigIfNotExists(Path path) throws IOException {
        if (IOHelper.isFile(path))
            return false;
        resetConfig(path);
        return true;
    }


    default boolean generateConfigIfNotExists() throws IOException {
        if (IOHelper.isFile(getPath()))
            return false;
        resetConfig();
        return true;
    }


    T getConfig();

    void setConfig(T config);

    T getDefaultConfig();

    Path getPath();

    Type getType();

}
