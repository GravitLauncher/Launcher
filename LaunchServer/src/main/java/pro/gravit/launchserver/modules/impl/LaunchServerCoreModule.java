package pro.gravit.launchserver.modules.impl;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.InitPhase;
import pro.gravit.utils.Version;

public class LaunchServerCoreModule extends LauncherModule {
    public LaunchServerCoreModule() {
        super(new LauncherModuleInfo("LaunchServerCore", Version.getVersion()));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::testEvent, InitPhase.class);
    }

    public void testEvent(InitPhase event) {
        //LogHelper.debug("[LaunchServerCore] Event LaunchServerInitPhase passed");
    }

    @Override
    public <T extends Event> boolean registerEvent(EventHandler<T> handle, Class<T> tClass) {
        return super.registerEvent(handle, tClass);
    }
}
