package ru.gravit.launchserver.components;

import ru.gravit.utils.helper.VerifyHelper;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Component {
    private static final Map<String, Class<? extends Component>> COMPONENTS = new ConcurrentHashMap<>(4);
    private static boolean registredComp = false;

    public static void registerHandler(String name, Class<? extends Component> adapter) {
        VerifyHelper.verifyIDName(name);
        VerifyHelper.putIfAbsent(COMPONENTS, name, Objects.requireNonNull(adapter, "adapter"),
                String.format("Auth handler has been already registered: '%s'", name));
    }

    public static Class<? extends Component> getHandlerClass(String name) {
        return COMPONENTS.get(name);
    }

    public static String getHandlerName(Class<Component> clazz) {
        for (Map.Entry<String, Class<? extends Component>> e : COMPONENTS.entrySet()) {
            if (e.getValue().equals(clazz)) return e.getKey();
        }
        return null;
    }

    public static void registerComponents() {
        if (!registredComp) {
            registredComp = true;
        }
    }
}
