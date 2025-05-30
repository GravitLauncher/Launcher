package pro.gravit.launcher.impl;

import org.junit.jupiter.api.Assertions;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.utils.Version;

public class MainModule extends LauncherModule {

    public MainModule() {
        super(new LauncherModuleInfoBuilder().setName("main").setVersion(new Version(1, 0, 0)).setPriority(0).setDependencies(new String[]{"depend1", "depend2"}).createLauncherModuleInfo());
    }

    @Override
    public void init(LauncherInitContext initContext) {
        Depend1Module module = modulesManager.getModule(Depend1Module.class);
        Assertions.assertEquals(InitStatus.FINISH, module.getInitStatus());
        Depend2Module module2 = modulesManager.getModule(Depend2Module.class);
        Assertions.assertEquals(InitStatus.FINISH, module2.getInitStatus());
    }
}
