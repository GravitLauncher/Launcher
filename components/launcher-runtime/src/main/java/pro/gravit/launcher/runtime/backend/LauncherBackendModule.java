package pro.gravit.launcher.runtime.backend;

import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfo;
import pro.gravit.launcher.core.backend.LauncherBackendAPIHolder;
import pro.gravit.launcher.runtime.client.events.ClientEngineInitPhase;
import pro.gravit.utils.Version;

public class LauncherBackendModule extends LauncherModule {
    private final LauncherBackendImpl backend = new LauncherBackendImpl();
    public LauncherBackendModule() {
        super(new LauncherModuleInfo("LauncherBackend", Version.getVersion()));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::onClientInitPhase, ClientEngineInitPhase.class);
    }

    private void onClientInitPhase(ClientEngineInitPhase phase) {
        LauncherBackendAPIHolder.setApi(backend);
    }
}
