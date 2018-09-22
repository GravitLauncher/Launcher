package ru.gravit.launcher.modules;


import ru.gravit.launcher.Launcher;
import ru.gravit.utils.Version;

public class TestClientModule implements Module {
    @Override
    public void close() throws Exception {

    }

    @Override
    public String getName() {
        return "TestClientModule";
    }

    @Override
    public Version getVersion() {
        return Launcher.getVersion();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void init(ModuleContext context) {

    }

    @Override
    public void postInit(ModuleContext context) {

    }

    @Override
    public void preInit(ModuleContext context) {

    }
}
