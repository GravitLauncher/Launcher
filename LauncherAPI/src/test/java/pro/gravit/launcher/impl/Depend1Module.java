package pro.gravit.launcher.impl;

import org.junit.jupiter.api.Assertions;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.utils.Version;

public class Depend1Module extends LauncherModule {
    public Depend1Module() {
        super(new LauncherModuleInfoBuilder().setName("depend1").setVersion(new Version(1, 0, 0)).setPriority(0).setDependencies(new String[]{"depend3", "internal", "virtual"}).createLauncherModuleInfo());
    }

    @Override
    public void init(LauncherInitContext initContext) {
        InternalModule module = modulesManager.getModule(InternalModule.class);
        Assertions.assertEquals(InitStatus.FINISH, module.getInitStatus());
        Depend3Module module1 = modulesManager.getModule(Depend3Module.class);
        Assertions.assertEquals(InitStatus.FINISH, module1.getInitStatus());
        VirtualInterface virtualInterface = modulesManager.getModuleByInterface(VirtualInterface.class);
        Assertions.assertEquals(InitStatus.FINISH, ((LauncherModule) virtualInterface).getInitStatus());
    }
}
