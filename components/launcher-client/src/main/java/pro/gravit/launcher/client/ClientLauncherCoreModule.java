package pro.gravit.launcher.client;

import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.utils.Version;

public class ClientLauncherCoreModule extends LauncherModule {
    public ClientLauncherCoreModule() {
        super(new LauncherModuleInfoBuilder().setName("ClientLauncherCore").setVersion(Version.getVersion()).createLauncherModuleInfo());
    }

    @Override
    public void init(LauncherInitContext initContext) {

    }
}
