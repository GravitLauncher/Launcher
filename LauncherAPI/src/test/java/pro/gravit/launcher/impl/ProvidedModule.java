package pro.gravit.launcher.impl;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
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
