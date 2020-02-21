package pro.gravit.launcher.impl;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.utils.Version;

public class CyclicDependModule extends LauncherModule {
    public CyclicDependModule() {
        super(new LauncherModuleInfo("cyclic1",
                new Version(1, 0, 0),
                2, new String[]{"cyclic2"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {

    }
}
