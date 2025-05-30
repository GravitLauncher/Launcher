package pro.gravit.launcher.impl;

import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;

public class Depend3Module extends LauncherModule {
    public Depend3Module() {
        super(new LauncherModuleInfoBuilder().setName("depend3").createLauncherModuleInfo());
    }

    @Override
    public void init(LauncherInitContext initContext) {
    }
}
