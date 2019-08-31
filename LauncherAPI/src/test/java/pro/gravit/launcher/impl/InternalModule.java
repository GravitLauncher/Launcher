package pro.gravit.launcher.impl;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;

public class InternalModule extends LauncherModule {
    public InternalModule() {
        super(new LauncherModuleInfo("internal"));
    }

    @Override
    public void init(LauncherInitContext initContext) {
    }
}
