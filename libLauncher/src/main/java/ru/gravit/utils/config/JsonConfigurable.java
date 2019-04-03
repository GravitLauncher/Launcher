package ru.gravit.utils.config;

import ru.gravit.launcher.Launcher;
import ru.gravit.utils.helper.IOHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;

public abstract class JsonConfigurable<T> {
    private Type type;
    protected Path configPath;

    public void saveConfig() throws IOException {
        saveConfig(configPath);
    }

    public void loadConfig() throws IOException {
        loadConfig(configPath);
    }

    public JsonConfigurable(Type type, Path configPath) {
        this.type = type;
        this.configPath = configPath;
    }

    public void saveConfig(Path configPath) throws IOException {
        try (BufferedWriter writer = IOHelper.newWriter(configPath)) {
            Launcher.gson.toJson(getConfig(), type, writer);
        }
    }

    public void loadConfig(Path configPath) throws IOException {
        if (generateConfigIfNotExists(configPath)) return;
        try (BufferedReader reader = IOHelper.newReader(configPath)) {
            setConfig(Launcher.gson.fromJson(reader, type));
        }
    }

    public void resetConfig() throws IOException {
        setConfig(getDefaultConfig());
        saveConfig();
    }

    public void resetConfig(Path newPath) throws IOException {
        setConfig(getDefaultConfig());
        saveConfig(newPath);
    }

    public boolean generateConfigIfNotExists(Path path) throws IOException {
        if (IOHelper.isFile(path))
            return false;
        resetConfig(path);
        return true;
    }

    public boolean generateConfigIfNotExists() throws IOException {
        if (IOHelper.isFile(configPath))
            return false;
        resetConfig();
        return true;
    }

    public abstract T getConfig();

    public abstract T getDefaultConfig();

    public abstract void setConfig(T config);
}
