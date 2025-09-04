package pro.gravit.launchserver.launchermodules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.core.LauncherTrustManager;
import pro.gravit.launcher.core.api.features.CoreFeatureAPI;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.asm.InjectClassAcceptor;
import pro.gravit.launchserver.binary.JARLauncherBinary;
import pro.gravit.launchserver.binary.tasks.MainBuildTask;
import pro.gravit.utils.helper.IOHelper;

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
    private final LaunchServer server;
    private volatile LauncherModuleClassLoader classLoader;
    private transient final Logger logger = LogManager.getLogger();

    public LauncherModuleLoader(LaunchServer server) {
        this.server = server;
    }

    public void init() {
        MainBuildTask mainTask = server.launcherBinaries.get(CoreFeatureAPI.UpdateVariant.JAR).getTaskByClass(MainBuildTask.class).get();
        mainTask.preBuildHook.registerHook((buildContext) -> {
            for (ModuleEntity e : launcherModules) {
                if (e.propertyMap != null) buildContext.properties.putAll(e.propertyMap);
                if(e.modernModule) {
                    buildContext.clientModules.add(e.moduleMainClass);
                } else {
                    buildContext.legacyClientModules.add(e.moduleMainClass);
                }
                buildContext.readerClassPath.add(new JarFile(e.path.toFile()));
            }
        });
        mainTask.postBuildHook.registerHook((buildContext) -> {
            for (ModuleEntity e : launcherModules) {
                logger.debug("Put {} launcher module", e.path.toString());
                buildContext.pushJarFile(e.path, (en) -> false, (en) -> true);
            }
        });
        try {
            loadLauncherModules();
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public void loadLauncherModules() throws IOException {
        launcherModules.clear();
        for(var e : server.modulesConfig.loadLauncherModules) {
            Path filePath = Path.of(e);
            if(!e.endsWith(".jar") && !Files.isDirectory(filePath)) {
                // It is in-bundle module
                filePath = server.modulesDir.resolve(e+".jar");
            }
            if(!Files.exists(filePath)) {
                logger.warn("Module {} not found", filePath);
            }
            if(Files.isDirectory(filePath)) {
                autoload(filePath);
            } else {
                registerModule(filePath);
            }
        }
    }

    public boolean registerModule(Path file) throws IOException {
        Attributes attributes;
        try (JarFile f = new JarFile(file.toFile())) {
            attributes = f.getManifest().getMainAttributes();
        }
        String mainClass = attributes.getValue("Module-Main-Class");
        if (mainClass == null) {
            logger.error("In module {} MainClass not found", file.toString());
            return false;
        }

        if (classLoader == null)
            classLoader = new LauncherModuleClassLoader(server.modulesManager.getModuleClassLoader());
        classLoader.addURL(file.toUri().toURL());
        ModuleEntity entity = new ModuleEntity();
        entity.path = file;
        entity.moduleMainClass = mainClass;
        entity.moduleConfigClass = attributes.getValue("Module-Config-Class");
        String requiredModernJava = attributes.getValue("Required-Modern-Java");
        entity.modernModule = Boolean.parseBoolean(requiredModernJava);
        if (entity.moduleConfigClass != null) {
            entity.moduleConfigName = attributes.getValue("Module-Config-Name");
            if (entity.moduleConfigName == null) {
                logger.warn("Module-Config-Name in module {} null. Module not configured", file.toString());
                return false;
            } else {
                try {
                    Class<?> clazz = classLoader.loadClass(entity.moduleConfigClass);
                    Path configPath = server.modulesManager.getConfigManager().getModuleConfig(entity.moduleConfigName);
                    Object defaultConfig = MethodHandles.publicLookup().findStatic(clazz, "getDefault", MethodType.methodType(Object.class)).invoke();
                    Object targetConfig;
                    if (!Files.exists(configPath)) {
                        logger.debug("Write default config for module {} to {}", file.toString(), configPath.toString());
                        try (Writer writer = IOHelper.newWriter(configPath)) {
                            Launcher.gsonManager.configGson.toJson(defaultConfig, writer);
                        }
                        targetConfig = defaultConfig;
                    } else {
                        try (Reader reader = IOHelper.newReader(configPath)) {
                            targetConfig = Launcher.gsonManager.configGson.fromJson(reader, clazz);
                        } catch (Exception e) {
                            logger.error("Error when reading config {} in module {}: {}", configPath, file, e);
                            return false;
                        }
                    }
                    if (entity.propertyMap == null) entity.propertyMap = new HashMap<>();
                    addClassFieldsToProperties(entity.propertyMap, "modules.".concat(entity.moduleConfigName.toLowerCase()), targetConfig, clazz);
                } catch (Throwable e) {
                    logger.error(e);
                }
            }
        }
        launcherModules.add(entity);
        return true;
    }

    public void autoload(Path modulesDir) throws IOException {
        IOHelper.walk(modulesDir, new ModulesVisitor(), false);
    }

    public void addClassFieldsToProperties(Map<String, Object> propertyMap, String prefix, Object object, Class<?> classOfObject) throws IllegalAccessException {
        Field[] fields = classOfObject.getFields();
        for (Field field : fields) {
            if ((field.getModifiers() & Modifier.STATIC) != 0) continue;
            Object obj = field.get(object);
            String propertyName = prefix.concat(".").concat(field.getName().toLowerCase(Locale.US));
            if (InjectClassAcceptor.isSerializableValue(obj)) {
                logger.trace("Property name {}", propertyName);
                propertyMap.put(propertyName, obj);
            } else {
                //Try recursive add fields
                addClassFieldsToProperties(propertyMap, propertyName, obj, obj.getClass());
            }
        }
    }

    public static class ModuleEntity {
        public Path path;
        public LauncherTrustManager.CheckClassResult checkResult;
        public String moduleMainClass;
        public String moduleConfigClass;
        public String moduleConfigName;
        public boolean modernModule;
        public Map<String, Object> propertyMap;
    }

    protected final class ModulesVisitor extends SimpleFileVisitor<Path> {
        private LauncherModuleClassLoader classLoader;

        private ModulesVisitor() {
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toFile().getName().endsWith(".jar"))
                registerModule(file);
            return super.visitFile(file, attrs);
        }
    }
}
