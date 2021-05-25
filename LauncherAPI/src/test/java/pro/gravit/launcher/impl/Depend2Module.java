package pro.gravit.launcher.impl;

import org.junit.jupiter.api.Assertions;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.utils.Version;

public class Depend2Module extends LauncherModule {
    public Depend2Module() {
        super(new LauncherModuleInfo("depend2"));
    }

    @Override
    public void preInitAction() {
        modulesManager.loadModule(new InternalModule());
        modulesManager.loadModule(new ProvidedModule());
    }

    @Override
    public void init(LauncherInitContext initContext) {
        requireModule("depend1", new Version(1, 0, 0));
        try {
            requireModule("dependNotFound", new Version(1, 0, 0));
            Assertions.fail("dependNotFound");
        } catch (RuntimeException ignored) {

        }
        try {
            requireModule("depend1", new Version(10, 0, 0));
            Assertions.fail("depend1 high version");
        } catch (RuntimeException ignored) {

        }
    }
}
