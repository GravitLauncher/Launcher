package pro.gravit.launcher.impl;

import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfo;
import pro.gravit.utils.Version;

public class ProvidedModule extends LauncherModule implements VirtualInterface {
    public ProvidedModule() {
        super(new LauncherModuleInfo("provided", new Version(1, 0, 0),
                0, new String[]{}, new String[]{"virtual"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {

    }
}
