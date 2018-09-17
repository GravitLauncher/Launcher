package ru.gravit.launchserver.manangers;

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
import ru.gravit.launcher.modules.SimpleModuleManager;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModulesManagerInterface;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.modules.CoreModule;
import ru.gravit.launchserver.modules.LaunchServerModuleContext;

public class ModulesManager extends SimpleModuleManager {
	public ModulesManager(LaunchServer lsrv) {
		modules = new ArrayList<>(1);
		classloader = new LauncherClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
		context = new LaunchServerModuleContext(lsrv, classloader);
	}
	private void registerCoreModule() {
		load(new CoreModule());
	}
}
