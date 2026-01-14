package pro.gravit.launchserver.modules.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.launcher.base.modules.events.InitPhase;
import pro.gravit.utils.Version;

public class LaunchServerCoreModule extends LauncherModule {

    private static final Logger logger =
            LoggerFactory.getLogger(LaunchServerCoreModule.class);

    public LaunchServerCoreModule() {
        super(new LauncherModuleInfoBuilder().setName("LaunchServerCore").setVersion(Version.getVersion()).createLauncherModuleInfo());
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::testEvent, InitPhase.class);
    }

    public void testEvent(InitPhase event) {
        //logger.debug("[LaunchServerCore] Event LaunchServerInitPhase passed");
    }

    @Override
    public <T extends Event> boolean registerEvent(EventHandler<T> handle, Class<T> tClass) {
        return super.registerEvent(handle, tClass);
    }
}