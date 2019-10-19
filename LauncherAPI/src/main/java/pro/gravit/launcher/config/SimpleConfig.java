package pro.gravit.launcher.config;

import java.lang.reflect.Type;
import java.nio.file.Path;

public abstract class SimpleConfig<T> implements JsonConfigurableInterface<T> {
    private transient final Class<T> type;
    protected transient final Path configPath;

    protected SimpleConfig(Class<T> type, Path configPath) {
        this.type = type;
        this.configPath = configPath;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getConfig() {
        return (T) this;
    }

    @Override
    public T getDefaultConfig() {
        try {
            return type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    @Override
    public Path getPath() {
        return configPath;
    }

    @Override
    public Type getType() {
        return type;
    }

}
