package ru.gravit.launchserver.modules;

import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.utils.Version;

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
    public Version getVersion() {
        return new Version(1, 0, 0, 0, Version.Type.UNKNOWN);
    }

    @Override
    public int getPriority() {
        return 0;
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
