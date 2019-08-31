package pro.gravit.launcher.impl;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;

public class Depend2Module extends LauncherModule {
    public Depend2Module() {
        super(new LauncherModuleInfo("depend2"));
    }

    @Override
    public void preInitAction() {
        modulesManager.loadModule(new InternalModule());
        modulesManager.loadModule(new ProvidedModule());
    }

    @Override
    public void init(LauncherInitContext initContext) {
    }
}
