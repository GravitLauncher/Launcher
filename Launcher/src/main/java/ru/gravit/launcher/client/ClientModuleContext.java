package ru.gravit.launcher.client;

import ru.gravit.launcher.LauncherEngine;
import ru.gravit.launcher.modules.ModuleContext;

public class ClientModuleContext implements ModuleContext {
    public final LauncherEngine engine;
    ClientModuleContext(LauncherEngine engine)
    {
        this.engine = engine;
    }
    @Override
    public Type getType() {
        return Type.CLIENT;
    }
}
