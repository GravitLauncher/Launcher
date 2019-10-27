package pro.gravit.launcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pro.gravit.launcher.impl.*;
import pro.gravit.launcher.impl.event.CancelEvent;
import pro.gravit.launcher.impl.event.NormalEvent;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.impl.SimpleModuleManager;

import java.nio.file.Path;

public class ModulesTest {
    @TempDir
    public static Path configDir;
    @TempDir
    public static Path modulesDir;
    public static SimpleModuleManager moduleManager;
    public static int dependInt = 0;

    public static void add(int a) {
        if (dependInt != a) throw new IllegalStateException(String.valueOf(a));
        dependInt++;
    }

    @BeforeAll
    public static void prepare() {
        moduleManager = new SimpleModuleManager(modulesDir, configDir);
    }

    @Test
    public void baseModule() {
        moduleManager.loadModule(new TestModule());
        moduleManager.initModules(null);
        NormalEvent e = new NormalEvent();
        moduleManager.invokeEvent(e);
        Assertions.assertTrue(e.passed);
        CancelEvent e1 = new CancelEvent();
        moduleManager.invokeEvent(e1);
        Assertions.assertTrue(e1.isCancel());
    }

    @Test
    public void dependenciesTest() {
        moduleManager.loadModule(new Depend1Module());
        moduleManager.loadModule(new Depend2Module());
        moduleManager.loadModule(new Depend3Module());
        moduleManager.loadModule(new MainModule());
        moduleManager.initModules(null);
        Assertions.assertEquals(moduleManager.getModule("depend1").getInitStatus(), LauncherModule.InitStatus.FINISH);
        Assertions.assertEquals(moduleManager.getModule("depend2").getInitStatus(), LauncherModule.InitStatus.FINISH);
        Assertions.assertEquals(moduleManager.getModule("depend3").getInitStatus(), LauncherModule.InitStatus.FINISH);
        Assertions.assertEquals(moduleManager.getModule("internal").getInitStatus(), LauncherModule.InitStatus.FINISH);
        Assertions.assertEquals(moduleManager.getModule("virtual").getInitStatus(), LauncherModule.InitStatus.FINISH);
        Assertions.assertEquals(moduleManager.getModule("main").getInitStatus(), LauncherModule.InitStatus.FINISH);
    }

    @Test
    public void cyclicTest() {
        moduleManager.loadModule(new CyclicDependModule());
        moduleManager.loadModule(new Cyclic2DependModule());
        moduleManager.initModules(null);
        Assertions.assertEquals(moduleManager.getModule("cyclic1").getInitStatus(), LauncherModule.InitStatus.FINISH);
        Assertions.assertEquals(moduleManager.getModule("cyclic2").getInitStatus(), LauncherModule.InitStatus.FINISH);
    }
}
