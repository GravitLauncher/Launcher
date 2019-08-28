package pro.gravit.launcher.modules.impl;

import pro.gravit.launcher.managers.SimpleModulesConfigManager;
import pro.gravit.launcher.modules.*;
import pro.gravit.utils.PublicURLClassLoader;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarFile;

public class SimpleModuleManager implements LauncherModulesManager {
    protected final List<LauncherModule> modules = new ArrayList<>();
    protected final List<String> moduleNames = new ArrayList<>();
    protected final SimpleModuleContext context;
    protected final ModulesConfigManager modulesConfigManager;
    protected final Path modulesDir;
    protected LauncherInitContext initContext;

    protected PublicURLClassLoader classLoader = new PublicURLClassLoader(new URL[]{});

    protected final class ModulesVisitor extends SimpleFileVisitor<Path> {
        private ModulesVisitor() {
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            loadModule(file);
            return super.visitFile(file, attrs);
        }
    }

    public void autoload() throws IOException {
        autoload(modulesDir);
    }

    public void autoload(Path dir) throws IOException {
        if (Files.notExists(dir)) Files.createDirectory(dir);
        else {
            IOHelper.walk(dir, new ModulesVisitor(), true);
        }
    }

    public void initModules(LauncherInitContext initContext) {
        List<LauncherModule> startedModules = Collections.unmodifiableList(new ArrayList<>(modules));
        for(LauncherModule module : startedModules)
        {
            module.preInit();
        }
        boolean isAnyModuleLoad = true;
        modules.sort((m1, m2) -> {
            int priority1 = m1.getModuleInfo().priority;
            int priority2 = m2.getModuleInfo().priority;
            return Integer.compare(priority1, priority2);
        });
        int modules_size = modules.size();
        int loaded = 0;
        while(isAnyModuleLoad)
        {
            isAnyModuleLoad = false;
            for(LauncherModule module : modules)
            {
                if(!module.getInitStatus().equals(LauncherModule.InitStatus.CREATED)) continue;
                if(checkDepend(module))
                {
                    isAnyModuleLoad = true;
                    module.setInitStatus(LauncherModule.InitStatus.INIT);
                    module.init(initContext);
                    module.setInitStatus(LauncherModule.InitStatus.FINISH);
                    loaded++;
                }
            }
            if(modules_size >= loaded) return;
        }
        for(LauncherModule module : modules)
        {
            if(module.getInitStatus().equals(LauncherModule.InitStatus.CREATED))
            {
                LauncherModuleInfo info = module.getModuleInfo();
                LogHelper.warning("Module %s required %s. Cyclic dependencies?", info.name, Arrays.toString(info.dependencies));
                module.setInitStatus(LauncherModule.InitStatus.INIT);
                module.init(initContext);
                module.setInitStatus(LauncherModule.InitStatus.FINISH);
            }
        }
    }

    private boolean checkDepend(LauncherModule module)
    {
        LauncherModuleInfo info = module.getModuleInfo();
        for(String dep : info.dependencies)
        {
            LauncherModule depModule = getModule(dep);
            if(depModule == null) throw new RuntimeException(String.format("Module %s required %s. %s not found", info.name, dep, dep));
            if(depModule.getInitStatus().equals(LauncherModule.InitStatus.CREATED)) return false;
        }
        return true;
    }

    public SimpleModuleManager(Path modulesDir, Path configDir) {
        modulesConfigManager = new SimpleModulesConfigManager(configDir);
        context = new SimpleModuleContext(this, modulesConfigManager);
        this.modulesDir = modulesDir;
    }

    @Override
    public LauncherModule loadModule(LauncherModule module) {
        if(modules.contains(module)) return module;
        modules.add(module);
        LauncherModuleInfo info = module.getModuleInfo();
        moduleNames.add(info.name);
        module.setContext(context);
        if(initContext != null)
        {
            module.preInit();
            module.init(initContext);
        }
        return module;
    }

    @Override
    public LauncherModule loadModule(Path file) throws IOException {
        try (JarFile f = new JarFile(file.toFile())) {
            String moduleClass = f.getManifest().getMainAttributes().getValue("Module-Main-Class");
            if(moduleClass == null)
            {
                LogHelper.error("In module %s Module-Main-Class not found", file.toString());
                return null;
            }
            classLoader.addURL(file.toUri().toURL());
            LauncherModule module = (LauncherModule) Class.forName(moduleClass, true, classLoader).newInstance();
            loadModule(module);
            return module;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            LogHelper.error(e);
            LogHelper.error("In module %s Module-Main-Class incorrect", file.toString());
            return null;
        }
    }

    @Override
    public LauncherModule getModule(String name) {
        for(LauncherModule module : modules)
        {
            LauncherModuleInfo info = module.getModuleInfo();
            if(info.name.equals(name)) return module;
        }
        return null;
    }

    @Override
    public LauncherModule getCoreModule() {
        return null;
    }

    @Override
    public ClassLoader getModuleClassLoader() {
        return classLoader;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LauncherModule> T getModule(Class<? extends T> clazz) {
        for(LauncherModule module : modules)
        {
            if(clazz.isAssignableFrom(module.getClass())) return (T) module;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LauncherModule> T findModule(Class<? extends T> clazz, Predicate<Version> versionPredicate) {
        for(LauncherModule module : modules)
        {
            LauncherModuleInfo info = module.getModuleInfo();
            if(!versionPredicate.test(info.version)) continue;
            if(clazz.isAssignableFrom(module.getClass())) return (T) module;
        }
        return null;
    }

    @Override
    public <T extends LauncherModule.Event> void invokeEvent(T event) {
        for(LauncherModule module : modules)
        {
            module.callEvent(event);
            if(event.isCancel()) return;
        }
    }
}
