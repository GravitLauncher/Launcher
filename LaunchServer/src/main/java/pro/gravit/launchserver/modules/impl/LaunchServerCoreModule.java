package pro.gravit.launchserver.modules.impl;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.modules.events.LaunchServerInitPhase;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

public class LaunchServerCoreModule extends LauncherModule {
    public LaunchServerCoreModule() {
        super(new LauncherModuleInfo("LaunchServerCore", Version.getVersion()));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::testEvent, LaunchServerInitPhase.class);
    }

    public void testEvent(LaunchServerInitPhase event)
    {
        LogHelper.debug("[LaunchServerCore] Event LaunchServerInitPhase passed");
    }
}
