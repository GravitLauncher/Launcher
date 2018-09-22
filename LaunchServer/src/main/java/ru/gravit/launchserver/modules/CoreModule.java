package ru.gravit.launchserver.modules;

import ru.gravit.launcher.Launcher;
import ru.gravit.utils.Version;
import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;

public class CoreModule implements Module {
    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public String getName() {
        return "LaunchServer";
    }

    @Override
    public Version getVersion() {
        return Launcher.getVersion();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void init(ModuleContext context) {
        // nothing to do
    }

    @Override
    public void postInit(ModuleContext context) {
        // nothing to do
    }


    @Override
    public void preInit(ModuleContext context) {
        // nothing to do
    }
}
