package pro.gravit.utils;

import pro.gravit.utils.helper.VerifyHelper;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The relationship between classes of an interface or abstract class and names when they are serialized
 *
 * @param <R> Class or interface type
 */
public class ProviderMap<R> {
    protected final Map<String, Class<? extends R>> PROVIDERS = new ConcurrentHashMap<>(4);
    protected final String name;
    protected boolean registredProviders = false;

    public ProviderMap(String name) {
        this.name = name;
    }

    public ProviderMap() {
        this.name = "Unnamed";
    }

    public String getName() {
        return name;
    }

    public void register(String name, Class<? extends R> adapter) {
        VerifyHelper.verifyIDName(name);
        VerifyHelper.putIfAbsent(PROVIDERS, name, Objects.requireNonNull(adapter, "adapter"),
                String.format("%s has been already registered: '%s'", this.name, name));
    }

    public Class<? extends R> getClass(String name) {
        return PROVIDERS.get(name);
    }

    public String getName(Class<? extends R> clazz) {
        for (Map.Entry<String, Class<? extends R>> e : PROVIDERS.entrySet()) {
            if (e.getValue().equals(clazz)) return e.getKey();
        }
        return null;
    }

    public Class<? extends R> unregister(String name) {
        return PROVIDERS.remove(name);
    }
}
