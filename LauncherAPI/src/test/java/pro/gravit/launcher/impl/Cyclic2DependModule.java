package pro.gravit.launcher.impl;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.utils.Version;

public class Cyclic2DependModule extends LauncherModule {
    public Cyclic2DependModule() {
        super(new LauncherModuleInfo("cyclic2",
                new Version(1, 0, 0),
                2, new String[]{"cyclic1"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {

    }
}
