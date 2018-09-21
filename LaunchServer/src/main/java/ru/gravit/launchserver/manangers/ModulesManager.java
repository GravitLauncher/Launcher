package ru.gravit.launchserver.manangers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.jar.JarFile;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherClassLoader;
import ru.gravit.launcher.modules.SimpleModuleManager;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.asm.ClassMetadataReader;
import ru.gravit.launchserver.modules.CoreModule;
import ru.gravit.launchserver.modules.LaunchServerModuleContext;
import ru.gravit.utils.helper.IOHelper;

public class ModulesManager extends SimpleModuleManager {
	public final ClassMetadataReader metadataReader;
	
	public ModulesManager(LaunchServer lsrv) {
		modules = new ArrayList<>(1);
		ClassMetadataReader metadataReader = null;
		try {
			metadataReader = new ClassMetadataReader(Collections.singletonList(new JarFile(new File(IOHelper.getCodeSource(Launcher.class).toUri()))));
		} catch (IOException e) { }
		this.metadataReader = metadataReader;
		classloader = new LauncherClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
		context = new LaunchServerModuleContext(lsrv, classloader, metadataReader);
		registerCoreModule();
	}
	
	private void registerCoreModule() {
		load(new CoreModule());
	}
	
	@Override
    @LauncherAPI
    public void loadModule(URL jarpath, String classname, boolean preload) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		super.loadModule(jarpath, classname, preload);
		try {
			if (metadataReader != null) metadataReader.add(new JarFile(new File(IOHelper.getCodeSource(Launcher.class).toUri())));
		} catch (IOException e) { }
	}
}
