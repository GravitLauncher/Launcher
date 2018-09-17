package ru.gravit.launchserver.modules;

import ru.gravit.launcher.LauncherVersion;
import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;

public class SimpleModule implements Module {
    @Override
	public void close() {
		// on stop
	}

    @Override
    public String getName() {
        return "SimpleModule";
    }

    @Override
    public LauncherVersion getVersion() {
        return new LauncherVersion(1,0,0);
    }

    @Override
    public void init(ModuleContext context) {

    }

    @Override
    public void postInit(ModuleContext context) {

    }


    @Override
    public void preInit(ModuleContext context) {

    }
}
