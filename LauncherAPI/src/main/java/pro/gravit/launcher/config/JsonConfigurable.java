package pro.gravit.launcher.config;

import pro.gravit.launcher.LauncherAPI;

import java.lang.reflect.Type;
import java.nio.file.Path;

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
