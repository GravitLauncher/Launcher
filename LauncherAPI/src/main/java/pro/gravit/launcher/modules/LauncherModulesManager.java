package pro.gravit.launcher.modules;

import pro.gravit.utils.Version;

import java.util.function.Predicate;

public interface LauncherModulesManager {
    LauncherModule loadModule(LauncherModule module);
    LauncherModule getModule(String name);
    default boolean containsModule(String name)
    {
        return getModule(name) != null;
    }

    default <T extends LauncherModule>  boolean containsModule(Class<? extends T> clazz)
    {
        return getModule(clazz) != null;
    }
    ClassLoader getModuleClassLoader();
    <T extends LauncherModule> T getModule(Class<? extends T> clazz);
    <T extends LauncherModule> T findModule(Class<? extends T> clazz, Predicate<Version> versionPredicate);
    <T extends LauncherModule.Event> void invokeEvent(T event) throws Exception;
}
