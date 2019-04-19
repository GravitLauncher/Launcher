package ru.gravit.launchserver.components;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.ProviderMap;

public abstract class Component {
    public static ProviderMap<Component> providers = new ProviderMap<>();
    private static boolean registredComp = false;

    public static void registerComponents() {
        if (!registredComp) {
            providers.registerProvider("authLimiter", AuthLimiterComponent.class);
            providers.registerProvider("commandRemover", CommandRemoverComponent.class);
            registredComp = true;
        }
    }

    public abstract void preInit(LaunchServer launchServer);

    public abstract void init(LaunchServer launchServer);

    public abstract void postInit(LaunchServer launchServer);
}
