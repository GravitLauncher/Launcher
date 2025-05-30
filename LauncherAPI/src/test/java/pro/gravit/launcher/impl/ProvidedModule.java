package pro.gravit.launcher.impl;

import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.utils.Version;

public class ProvidedModule extends LauncherModule implements VirtualInterface {
    public ProvidedModule() {
        super(new LauncherModuleInfoBuilder().setName("provided").setVersion(new Version(1, 0, 0)).setPriority(0).setDependencies(new String[]{}).setProviders(new String[]{"virtual"}).createLauncherModuleInfo());
    }

    @Override
    public void init(LauncherInitContext initContext) {

    }
}
