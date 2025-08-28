package pro.gravit.launcher.impl;

import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;

public class InternalModule extends LauncherModule {
    public InternalModule() {
        super(new LauncherModuleInfoBuilder().setName("internal").createLauncherModuleInfo());
    }

    @Override
    public void init(LauncherInitContext initContext) {
    }
}
