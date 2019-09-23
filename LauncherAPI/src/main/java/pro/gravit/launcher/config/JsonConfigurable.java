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

public abstract class JsonConfigurable<T> implements JsonConfigurableInterface<T> {
    private transient final Type type;
    protected transient final Path configPath;

    @LauncherAPI
    public JsonConfigurable(Type type, Path configPath) {
        this.type = type;
        this.configPath = configPath;
    }

    @Override
    public Path getPath() {
        return configPath;
    }

    @Override
    public Type getType() {
        return type;
    }

    @LauncherAPI
    public abstract T getConfig();

    @LauncherAPI
    public abstract T getDefaultConfig();

    @LauncherAPI
    public abstract void setConfig(T config);
}
