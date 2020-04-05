package pro.gravit.launcher.config;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;
import java.nio.file.Path;

public abstract class SimpleConfig<T> implements JsonConfigurableInterface<T> {
    protected transient final Path configPath;
    private transient final Class<T> type;

    protected SimpleConfig(Class<T> type, Path configPath) {
        this.type = type;
        this.configPath = configPath;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getConfig() {
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getDefaultConfig() {
        try {
            return (T) MethodHandles.publicLookup().findConstructor(type, MethodType.methodType(void.class)).invokeWithArguments();
        } catch (Throwable e) {
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
