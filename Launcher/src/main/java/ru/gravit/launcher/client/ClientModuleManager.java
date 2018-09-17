package ru.gravit.launcher.client;

import java.net.URL;
import java.util.ArrayList;

import ru.gravit.launcher.LauncherEngine;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModulesManagerInterface;

public class ClientModuleManager implements ModulesManagerInterface,AutoCloseable {
    public ArrayList<Module> modules;
    private final ClientModuleContext context;
    public ClientModuleManager(LauncherEngine engine)
    {
        context = new ClientModuleContext(engine);
        modules = new ArrayList<>();
    }
    @Override
    public void close() throws Exception {
        for (Module m : modules)
			try {
                m.close();
            } catch (Throwable t) {
                if (m.getName() != null)
                    LogHelper.error("Error in stopping module: %s", m.getName());
                else
                    LogHelper.error("Error in stopping one of modules");
                LogHelper.error(t);
            }
    }
    @Override
    public void initModules() throws Exception {
        for (Module m : modules) {
            m.init(context);
            LogHelper.info("Module %s version: %s init", m.getName(), m.getVersion());
        }
    }

    @Override
    public void load(Module module) throws Exception {
        modules.add(module);
    }

    @Override
    public void load(Module module, boolean preload) throws Exception {
        modules.add(module);
        if(!preload) {
            module.preInit(context);
            module.init(context);
            module.postInit(context);
        }
    }

    @Override
    public void loadModule(URL jarpath, boolean preload) throws Exception {
        throw new SecurityException("Custom JAR's load not allowed here");
    }

    @Override
    public void loadModule(URL jarpath, String classname, boolean preload) throws Exception {
        throw new SecurityException("Custom JAR's load not allowed here");
    }

    @Override
    public void postInitModules() throws Exception {
        for (Module m : modules) {
            m.postInit(context);
            LogHelper.info("Module %s version: %s post-init", m.getName(), m.getVersion());
        }
    }

    @Override
    public void preInitModules() throws Exception {
        for (Module m : modules) {
            m.preInit(context);
            LogHelper.info("Module %s version: %s pre-init", m.getName(), m.getVersion());
        }
    }

    @Override
    public void printModules() throws Exception {
        for (Module m : modules)
			LogHelper.info("Module %s version: %s", m.getName(), m.getVersion());
        LogHelper.info("Loaded %d modules", modules.size());
    }

    @Override
    public void registerModule(Module module, boolean preload) throws Exception {
        modules.add(module);
        if(!preload) {
            module.preInit(context);
            module.init(context);
            module.postInit(context);
        }
    }
}
