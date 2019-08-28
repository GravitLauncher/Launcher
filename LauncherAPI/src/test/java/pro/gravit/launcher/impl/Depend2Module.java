package pro.gravit.launcher.impl;

import pro.gravit.launcher.ModulesTest;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;

public class Depend2Module extends LauncherModule {
    public Depend2Module() {
        super(new LauncherModuleInfo("depend2"));
    }

    @Override
    public void preInit() {
        super.preInit();
        modulesManager.loadModule(new InternalModule());
    }

    @Override
    public void init(LauncherInitContext initContext) {
    }
}
