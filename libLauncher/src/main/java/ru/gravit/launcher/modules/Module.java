package ru.gravit.launcher.modules;

import ru.gravit.utils.Version;

public interface Module extends AutoCloseable {

    String getName();

    Version getVersion();

    int getPriority();

    void init(ModuleContext context);

    void postInit(ModuleContext context);

    void preInit(ModuleContext context);

    default void finish(ModuleContext context) {
    	// NOP
    };
}
