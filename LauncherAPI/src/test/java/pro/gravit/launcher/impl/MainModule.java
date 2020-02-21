package pro.gravit.launcher.impl;

import org.junit.jupiter.api.Assertions;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.utils.Version;

public class MainModule extends LauncherModule {

    public MainModule() {
        super(new LauncherModuleInfo("main",
                new Version(1, 0, 0),
                0, new String[]{"depend1", "depend2"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        Depend1Module module = modulesManager.getModule(Depend1Module.class);
        Assertions.assertEquals(module.getInitStatus(), InitStatus.FINISH);
        Depend2Module module2 = modulesManager.getModule(Depend2Module.class);
        Assertions.assertEquals(module2.getInitStatus(), InitStatus.FINISH);
    }
}
