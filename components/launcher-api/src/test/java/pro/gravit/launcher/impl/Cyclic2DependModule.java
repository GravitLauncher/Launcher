package pro.gravit.launcher.impl;

import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.utils.Version;

public class Cyclic2DependModule extends LauncherModule {
    public Cyclic2DependModule() {
        super(new LauncherModuleInfoBuilder().setName("cyclic2").setVersion(new Version(1, 0, 0)).setPriority(2).setDependencies(new String[]{"cyclic1"}).createLauncherModuleInfo());
    }

    @Override
    public void init(LauncherInitContext initContext) {

    }
}
