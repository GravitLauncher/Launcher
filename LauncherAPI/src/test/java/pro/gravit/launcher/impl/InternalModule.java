package pro.gravit.launcher.impl;

import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfo;

public class InternalModule extends LauncherModule {
    public InternalModule() {
        super(new LauncherModuleInfo("internal"));
    }

    @Override
    public void init(LauncherInitContext initContext) {
    }
}
