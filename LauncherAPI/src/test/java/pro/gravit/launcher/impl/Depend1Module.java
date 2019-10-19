package pro.gravit.launcher.impl;

import org.junit.jupiter.api.Assertions;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.utils.Version;

public class Depend1Module extends LauncherModule {
    public Depend1Module() {
        super(new LauncherModuleInfo("depend1", new Version(1, 0, 0),
                0,
                new String[]{"depend3", "internal", "virtual"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        InternalModule module = modulesManager.getModule(InternalModule.class);
        Assertions.assertEquals(module.getInitStatus(), InitStatus.FINISH);
        Depend3Module module1 = modulesManager.getModule(Depend3Module.class);
        Assertions.assertEquals(module1.getInitStatus(), InitStatus.FINISH);
        VirtualInterface virtualInterface = modulesManager.getModuleByInterface(VirtualInterface.class);
        Assertions.assertEquals(((LauncherModule) virtualInterface).getInitStatus(), InitStatus.FINISH);
    }
}
