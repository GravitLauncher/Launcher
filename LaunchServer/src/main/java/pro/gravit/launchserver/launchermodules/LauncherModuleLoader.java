package pro.gravit.launchserver.launchermodules;

import pro.gravit.launcher.Launcher;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.asm.InjectClassAcceptor;
import pro.gravit.launchserver.binary.tasks.MainBuildTask;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public class LauncherModuleLoader {
    public final List<ModuleEntity> launcherModules = new ArrayList<>();
    public final Path modulesDir;
    private final LaunchServer server;

    public LauncherModuleLoader(LaunchServer server) {
        this.server = server;
        modulesDir = server.dir.resolve("launcher-modules");
    }

    public void init() {
        if (!IOHelper.isDir(modulesDir)) {
            try {
                Files.createDirectories(modulesDir);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        server.commandHandler.registerCommand("syncLauncherModules", new SyncLauncherModulesCommand(this));
        MainBuildTask mainTask = server.launcherBinary.getTaskByClass(MainBuildTask.class).get();
        mainTask.preBuildHook.registerHook((buildContext) -> {
            for (ModuleEntity e : launcherModules) {
                if (e.propertyMap != null) buildContext.task.properties.putAll(e.propertyMap);
                buildContext.clientModules.add(e.moduleMainClass);
                buildContext.readerClassPath.add(new JarFile(e.path.toFile()));
            }
        });
        mainTask.postBuildHook.registerHook((buildContext) -> {
            for (ModuleEntity e : launcherModules) {
                LogHelper.debug("Put %s launcher module", e.path.toString());
                buildContext.pushJarFile(e.path, (en) -> false, (en) -> true);
            }
        });
        try {
            syncModules();
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    public void syncModules() throws IOException {
        launcherModules.clear();
        IOHelper.walk(modulesDir, new ModulesVisitor(), false);
    }

    static class ModuleEntity {
        public Path path;
        public String moduleMainClass;
        public String moduleConfigClass;
        public String moduleConfigName;
        public Map<String, Object> propertyMap;
    }

    protected final class ModulesVisitor extends SimpleFileVisitor<Path> {
        private LauncherModuleClassLoader classLoader;

        private ModulesVisitor() {
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toFile().getName().endsWith(".jar"))
                try (JarFile f = new JarFile(file.toFile())) {
                    Attributes attributes = f.getManifest().getMainAttributes();
                    String mainClass = attributes.getValue("Module-Main-Class");
                    if (mainClass == null) {
                        LogHelper.error("In module %s MainClass not found", file.toString());
                    } else {
                        ModuleEntity entity = new ModuleEntity();
                        entity.path = file;
                        entity.moduleMainClass = mainClass;
                        entity.moduleConfigClass = attributes.getValue("Module-Config-Class");
                        if (entity.moduleConfigClass != null) {
                            entity.moduleConfigName = attributes.getValue("Module-Config-Name");
                            if (entity.moduleConfigName == null) {
                                LogHelper.warning("Module-Config-Name in module %s null. Module not configured", file.toString());
                            } else {
                                try {
                                    if (classLoader == null)
                                        classLoader = new LauncherModuleClassLoader(server.modulesManager.getModuleClassLoader());
                                    classLoader.addURL(file.toUri().toURL());
                                    Class<?> clazz = classLoader.loadClass(entity.moduleConfigClass);
                                    Path configPath = server.modulesManager.getConfigManager().getModuleConfig(entity.moduleConfigName);
                                    Object defaultConfig = MethodHandles.publicLookup().findStatic(clazz, "getDefault", MethodType.methodType(Object.class)).invoke();
                                    Object targetConfig;
                                    if (!Files.exists(configPath)) {
                                        LogHelper.debug("Write default config for module %s to %s", file.toString(), configPath.toString());
                                        try (Writer writer = IOHelper.newWriter(configPath)) {
                                            Launcher.gsonManager.configGson.toJson(defaultConfig, writer);
                                        }
                                        targetConfig = defaultConfig;
                                    } else {
                                        try (Reader reader = IOHelper.newReader(configPath)) {
                                            targetConfig = Launcher.gsonManager.configGson.fromJson(reader, clazz);
                                        }
                                    }
                                    //Field[] fields = clazz.getFields();
                                    //for (Field field : fields) {
                                    //    if ((field.getModifiers() & Modifier.STATIC) != 0) continue;
                                    //    Object obj = field.get(targetConfig);
                                    //    String configPropertyName = "modules.".concat(entity.moduleConfigName.toLowerCase()).concat(".").concat(field.getName().toLowerCase());
                                    //    if (entity.propertyMap == null) entity.propertyMap = new HashMap<>();
                                    //    LogHelper.dev("Property name %s", configPropertyName);
                                    //    entity.propertyMap.put(configPropertyName, obj);
                                    //}
                                    if (entity.propertyMap == null) entity.propertyMap = new HashMap<>();
                                    addClassFieldsToProperties(entity.propertyMap, "modules.".concat(entity.moduleConfigName.toLowerCase()), targetConfig, clazz);
                                } catch (Throwable e) {
                                    LogHelper.error(e);
                                }
                            }
                        }
                        launcherModules.add(entity);
                    }
                }
            return super.visitFile(file, attrs);
        }
    }

    public void addClassFieldsToProperties(Map<String, Object> propertyMap, String prefix, Object object, Class<?> classOfObject) throws IllegalAccessException {
        Field[] fields = classOfObject.getFields();
        for (Field field : fields) {
            if ((field.getModifiers() & Modifier.STATIC) != 0) continue;
            Object obj = field.get(object);
            String propertyName = prefix.concat(".").concat(field.getName().toLowerCase(Locale.US));
            if (InjectClassAcceptor.isSerializableValue(obj)) {
                LogHelper.dev("Property name %s", propertyName);
                propertyMap.put(propertyName, obj);
            } else {
                //Try recursive add fields
                addClassFieldsToProperties(propertyMap, propertyName, obj, obj.getClass());
            }
        }
    }
}
