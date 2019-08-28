package pro.gravit.launcher.modules;

import pro.gravit.utils.Version;
@Deprecated
public interface Module extends AutoCloseable {

    String getName();

    Version getVersion();

    int getPriority();

    void init(ModuleContext context);

    void postInit(ModuleContext context);

    void preInit(ModuleContext context);

    default void finish(ModuleContext context) {
        // NOP
    }
}
