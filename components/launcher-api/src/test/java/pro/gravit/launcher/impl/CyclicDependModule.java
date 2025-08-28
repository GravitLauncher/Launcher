package pro.gravit.launcher.impl;

import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.utils.Version;

public class CyclicDependModule extends LauncherModule {
    public CyclicDependModule() {
        super(new LauncherModuleInfoBuilder().setName("cyclic1").setVersion(new Version(1, 0, 0)).setPriority(2).setDependencies(new String[]{"cyclic2"}).createLauncherModuleInfo());
    }

    @Override
    public void init(LauncherInitContext initContext) {

    }
}
