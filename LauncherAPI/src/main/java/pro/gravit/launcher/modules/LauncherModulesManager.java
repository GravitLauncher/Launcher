package pro.gravit.launcher.modules;

import pro.gravit.utils.Version;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

public interface LauncherModulesManager {

    LauncherModule loadModule(LauncherModule module);

    LauncherModule loadModule(Path file) throws IOException;

    LauncherModule getModule(String name);

    LauncherModule getCoreModule();

    default boolean containsModule(String name) {
        return getModule(name) != null;
    }

    default <T extends LauncherModule> boolean containsModule(Class<? extends T> clazz) {
        return getModule(clazz) != null;
    }

    ClassLoader getModuleClassLoader();

    ModulesConfigManager getConfigManager();

    <T extends LauncherModule> T getModule(Class<? extends T> clazz);

    <T> T getModuleByInterface(Class<T> clazz);

    <T> List<T> getModulesByInterface(Class<T> clazz);

    <T extends LauncherModule> T findModule(Class<? extends T> clazz, Predicate<Version> versionPredicate);

    LauncherModule findModule(String name, Predicate<Version> versionPredicate);

    /**
     * Invoke event processing for all modules.
     * Event processing is carried out in the order of the modules in the list (sorted by priority)
     *
     * @param event event handled
     * @param <T>   event type
     */
    <T extends LauncherModule.Event> void invokeEvent(T event);
}
