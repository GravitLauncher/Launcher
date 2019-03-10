package ru.gravit.launcher.managers;

import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launcher.modules.ModulesManager;
import ru.gravit.utils.PublicURLClassLoader;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.jar.JarFile;

public class SimpleModuleManager implements ModulesManager {
    protected final class ModulesVisitor extends SimpleFileVisitor<Path> {
        private ModulesVisitor() {
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        	if (file.toFile().getName().endsWith(".jar"))
        		try (JarFile f = new JarFile(file.toFile())) {
                	loadModule(file.toUri().toURL(), f.getManifest().getMainAttributes().getValue("Main-Class"));
            	} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                	LogHelper.error(e);
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

    public void loadModuleFull(URL jarpath) throws ClassNotFoundException, IllegalAccessException, InstantiationException, URISyntaxException, IOException {
    	try (JarFile f = new JarFile(Paths.get(jarpath.toURI()).toFile())) {
    		classloader.addURL(jarpath);
        	Module module = (Module) Class.forName(f.getManifest().getMainAttributes().getValue("Main-Class"), true, classloader).newInstance();
        	modules.add(module);
        	module.preInit(context);
        	module.init(context);
        	module.postInit(context);
        	module.finish(context);
        	LogHelper.info("Module %s version: %s loaded", module.getName(), module.getVersion());
    	}
    }

	@Override
	public void loadModule(URL jarpath) throws Exception {
		try (JarFile f = new JarFile(Paths.get(jarpath.toURI()).toFile())) {
            loadModule(jarpath, f.getManifest().getMainAttributes().getValue("Main-Class")); 
        }
	}

    @Override
    public void loadModule(URL jarpath, String classname) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        classloader.addURL(jarpath);
        Module module = (Module) Class.forName(classname, true, classloader).newInstance();
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
    public void registerModule(Module module) {
        modules.add(module);
        LogHelper.info("Module %s version: %s registered", module.getName(), module.getVersion());
    }

	@Override
	public void finishModules() {
        for (Module m : modules) {
            m.finish(context);
            LogHelper.info("Module %s version: %s finished initialization", m.getName(), m.getVersion());
        }
	}
}
