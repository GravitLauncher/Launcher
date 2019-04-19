package ru.gravit.utils;

import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ProviderMap<R> {
    protected final Map<String, Class<? extends R>> PROVIDERS = new ConcurrentHashMap<>(4);
    protected boolean registredProviders = false;


    public void registerProvider(String name, Class<? extends R> adapter) {
        VerifyHelper.verifyIDName(name);
        VerifyHelper.putIfAbsent(PROVIDERS, name, Objects.requireNonNull(adapter, "adapter"),
                String.format("Protect handler has been already registered: '%s'", name));
    }

    public Class<? extends R> getProviderClass(String name) {
        return PROVIDERS.get(name);
    }

    public String getProviderName(Class<? extends R> clazz) {
        for (Map.Entry<String, Class<? extends R>> e : PROVIDERS.entrySet()) {
            if (e.getValue().equals(clazz)) return e.getKey();
        }
        return null;
    }
    public Class<? extends R> unregisterProvider(String name)
    {
        return PROVIDERS.remove(name);
    }
}
