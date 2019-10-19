package pro.gravit.launcher.impl;

import pro.gravit.launcher.impl.event.CancelEvent;
import pro.gravit.launcher.impl.event.NormalEvent;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;

public class TestModule extends LauncherModule {

    public TestModule() {
        super(new LauncherModuleInfo("testModule"));
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
