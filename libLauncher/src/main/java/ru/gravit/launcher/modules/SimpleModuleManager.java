package ru.gravit.launcher.modules;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import ru.gravit.utils.PublicURLClassLoader;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

public class SimpleModuleManager implements ModulesManagerInterface, AutoCloseable {
    protected final class ModulesVisitor extends SimpleFileVisitor<Path> {
        private ModulesVisitor() {
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            try {
                JarFile f = new JarFile(file.toFile());
                Manifest m = f.getManifest();
                String mainclass = m.getMainAttributes().getValue("Main-Class");
                loadModule(file.toUri().toURL(), mainclass);
                f.close();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }

            return super.visitFile(file, attrs);
        }
    }

    public ArrayList<Module> modules;
    public PublicURLClassLoader classloader;
    protected ModuleContext context;


    public void autoload(Path dir) throws IOException {
        LogHelper.info("Load modules");
        if (Files.notExists(dir)) Files.createDirectory(dir);
        IOHelper.walk(dir, new ModulesVisitor(), true);
        sort();
        LogHelper.info("Loaded %d modules", modules.size());
    }

    public void sort() {
        modules.sort((m1, m2) -> {
            int p1 = m1.getPriority();
            int p2 = m2.getPriority();
            return Integer.compare(p2, p1);
        });
    }

    @Override
    public void close() {
        for (Module m : modules)
            try {
                m.close();
            } catch (Throwable t) {
                if (m.getName() != null) LogHelper.error("Error in stopping module: %s", m.getName());
                else LogHelper.error("Error in stopping one of modules");
                LogHelper.error(t);
            }
    }

    @Override

    public void initModules() {
        for (Module m : modules) {
            m.init(context);
            LogHelper.info("Module %s version: %s init", m.getName(), m.getVersion());
        }
    }

    @Override

    public void load(Module module) {
        modules.add(module);
    }


    @Override

    public void loadModule(URL jarpath) throws ClassNotFoundException, IllegalAccessException, InstantiationException, URISyntaxException, IOException {
        JarFile f = new JarFile(Paths.get(jarpath.toURI()).toString());
        Manifest m = f.getManifest();
        String mainclass = m.getMainAttributes().getValue("Main-Class");
        loadModule(jarpath, mainclass);
        f.close();
    }

    public void loadModuleFull(URL jarpath) throws ClassNotFoundException, IllegalAccessException, InstantiationException, URISyntaxException, IOException {
        JarFile f = new JarFile(Paths.get(jarpath.toURI()).toString());
        Manifest m = f.getManifest();
        String mainclass = m.getMainAttributes().getValue("Main-Class");
        classloader.addURL(jarpath);
        Class<?> moduleclass = Class.forName(mainclass, true, classloader);
        Module module = (Module) moduleclass.newInstance();
        modules.add(module);
        module.preInit(context);
        module.init(context);
        module.postInit(context);
        LogHelper.info("Module %s version: %s loaded", module.getName(), module.getVersion());
        f.close();
    }
    
    @Override

    public void loadModule(URL jarpath, String classname) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        classloader.addURL(jarpath);
        Class<?> moduleclass = Class.forName(classname, true, classloader);
        Module module = (Module) moduleclass.newInstance();
        modules.add(module);
        LogHelper.info("Module %s version: %s loaded", module.getName(), module.getVersion());
    }

    @Override
    public void postInitModules() {
        for (Module m : modules) {
            m.postInit(context);
            LogHelper.info("Module %s version: %s post-init", m.getName(), m.getVersion());
        }
    }


    @Override

    public void preInitModules() {
        for (Module m : modules) {
            m.preInit(context);
            LogHelper.info("Module %s version: %s pre-init", m.getName(), m.getVersion());
        }
    }

    @Override

    public void printModules() {
        for (Module m : modules)
            LogHelper.info("Module %s version: %s", m.getName(), m.getVersion());
        LogHelper.info("Loaded %d modules", modules.size());
    }

    @Override

    public void registerModule(Module module, boolean preload) {
        load(module);
        LogHelper.info("Module %s version: %s registered", module.getName(), module.getVersion());
    }
}
