package ru.gravit.launcher.server;

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

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherClassLoader;
import ru.gravit.launcher.helper.IOHelper;
import ru.gravit.launcher.helper.LogHelper;
import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModulesManagerInterface;

public class ModulesManager implements AutoCloseable, ModulesManagerInterface {
    private final class ModulesVisitor extends SimpleFileVisitor<Path> {
        private ModulesVisitor() {
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            try {
                JarFile f = new JarFile(file.toString());
                Manifest m = f.getManifest();
                String mainclass = m.getMainAttributes().getValue("Main-Class");
                loadModule(file.toUri().toURL(), mainclass, true);
                f.close();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }

            // Return result
            return super.visitFile(file, attrs);
        }
    }
    public ArrayList<Module> modules;
	public LauncherClassLoader classloader;

    private final ServerModuleContext context;
    
    public ModulesManager(ServerWrapper wrapper) {
        modules = new ArrayList<>();
        classloader = new LauncherClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        context = new ServerModuleContext(wrapper, classloader);
    }

    @LauncherAPI
    public void autoload(Path dir) throws IOException {
        LogHelper.info("Load modules");
        if (Files.notExists(dir)) Files.createDirectory(dir);
        IOHelper.walk(dir, new ModulesVisitor(), true);
        LogHelper.info("Loaded %d modules", modules.size());
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
	@LauncherAPI
    public void initModules() {
        for (Module m : modules) {
            m.init(context);
            LogHelper.info("Module %s version: %s init", m.getName(), m.getVersion());
        }
    }
    
    @Override
	@LauncherAPI
	public void load(Module module) {
		modules.add(module);
	}

    @Override
	@LauncherAPI
	public void load(Module module, boolean preload) {
		load(module);
		if (!preload) module.init(context);
	}

    
    @Override
	@LauncherAPI
    public void loadModule(URL jarpath, boolean preload) throws ClassNotFoundException, IllegalAccessException, InstantiationException, URISyntaxException, IOException {
        JarFile f = new JarFile(Paths.get(jarpath.toURI()).toString());
        Manifest m = f.getManifest();
        String mainclass = m.getMainAttributes().getValue("Main-Class");
        loadModule(jarpath, mainclass, preload);
        f.close();
    }

    @Override
	@LauncherAPI
    public void loadModule(URL jarpath, String classname, boolean preload) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        classloader.addURL(jarpath);
        Class<?> moduleclass = Class.forName(classname, true, classloader);
        Module module = (Module) moduleclass.newInstance();
        modules.add(module);
        module.preInit(context);
        if(!preload) module.init(context);
        LogHelper.info("Module %s version: %s loaded",module.getName(),module.getVersion());
    }
    
    @Override
	public void postInitModules() {
        for (Module m : modules) {
            m.postInit(context);
            LogHelper.info("Module %s version: %s post-init", m.getName(), m.getVersion());
        }
	}
    
    
    @Override
	@LauncherAPI
    public void preInitModules() {
        for (Module m : modules) {
            m.preInit(context);
            LogHelper.info("Module %s version: %s pre-init", m.getName(), m.getVersion());
        }
    }

    @Override
	@LauncherAPI
    public void printModules() {
        for (Module m : modules)
			LogHelper.info("Module %s version: %s", m.getName(), m.getVersion());
        LogHelper.info("Loaded %d modules", modules.size());
    }

	@Override
	@LauncherAPI
    public void registerModule(Module module,boolean preload)
    {
        load(module, preload);
        LogHelper.info("Module %s version: %s registered",module.getName(),module.getVersion());
    }
}
