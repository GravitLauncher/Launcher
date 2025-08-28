package pro.gravit.launcher.impl;

import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.launcher.impl.event.CancelEvent;
import pro.gravit.launcher.impl.event.NormalEvent;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;

public class TestModule extends LauncherModule {

    public TestModule() {
        super(new LauncherModuleInfoBuilder().setName("testModule").createLauncherModuleInfo());
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::testevent, NormalEvent.class);
        registerEvent(this::testevent2, CancelEvent.class);
    }

    public void testevent(NormalEvent event) {
        event.passed = true;
    }

    public void testevent2(CancelEvent cancelEvent) {
        cancelEvent.cancel();
    }
}
