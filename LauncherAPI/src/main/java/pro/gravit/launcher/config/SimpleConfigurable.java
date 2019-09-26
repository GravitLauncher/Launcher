package pro.gravit.launcher.config;

import java.nio.file.Path;

public class SimpleConfigurable<T> extends JsonConfigurable<T> {
    public T config;
    private final Class<T> tClass;

    public SimpleConfigurable(Class<T> type, Path configPath) {
        super(type, configPath);
        tClass = type;
    }

    @Override
    public T getConfig() {
        return config;
    }

    @Override
    public T getDefaultConfig() {
        try {
            return tClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    @Override
    public void setConfig(T config) {
        this.config = config;
    }
}
