package pro.gravit.launchserver.modules;

import pro.gravit.launcher.modules.Module;
import pro.gravit.launcher.modules.ModuleContext;
import pro.gravit.utils.Version;

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
        return Version.getVersion();
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
