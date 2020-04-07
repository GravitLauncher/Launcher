package pro.gravit.launcher.config;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;

public class SimpleConfigurable<T> extends JsonConfigurable<T> {
    private final Class<T> tClass;
    public T config;

    public SimpleConfigurable(Class<T> type, Path configPath) {
        super(type, configPath);
        tClass = type;
    }

    @Override
    public T getConfig() {
        return config;
    }

    @Override
    public void setConfig(T config) {
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getDefaultConfig() {
        try {
            return (T) MethodHandles.publicLookup().findConstructor(tClass, MethodType.methodType(void.class)).invokeWithArguments();
        } catch (Throwable e) {
            return null;
        }
    }
}
