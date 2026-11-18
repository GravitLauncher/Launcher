package pro.gravit.launcher.start;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.modules.impl.ClasspathModuleManager;
import pro.gravit.launcher.base.modules.LauncherModule;

public final class RuntimeModuleManager extends ClasspathModuleManager {
    public RuntimeModuleManager() {
        this(false);
    }

    public RuntimeModuleManager(boolean fileLoading) {
        super(Launcher.getConfig().trustManager, fileLoading);
    }

    public void callWrapper(ClientLauncherWrapper.ClientLauncherWrapperContext context) {
        for (LauncherModule module : modules) {
            if (module instanceof ClientWrapperModule) {
                ((ClientWrapperModule) module).wrapperPhase(context);
            }
        }
    }
}
