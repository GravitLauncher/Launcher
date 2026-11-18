package pro.gravit.launcher.client;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.modules.impl.ClasspathModuleManager;

public final class ClientModuleManager extends ClasspathModuleManager {
    public ClientModuleManager() {
        super(Launcher.getConfig().trustManager, false);
    }


}
