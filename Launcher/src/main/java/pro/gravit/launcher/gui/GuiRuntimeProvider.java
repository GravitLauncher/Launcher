package pro.gravit.launcher.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherAPI;
import pro.gravit.launcher.sgui.api.GuiEngine;
import pro.gravit.launcher.sgui.Application;
import pro.gravit.utils.helper.LogHelper;

public class GuiRuntimeProvider implements RuntimeProvider {

    private final GuiEngine engine = GuiEngine.getGuiEngine();
    private boolean isPreLoaded = false;

    @LauncherAPI
    public void loadConfig(String get) {
		engine.config(engine, get);
    }

    @Override
    public void run(String[] args) throws NoSuchMethodException, IOException {
        preLoad();
		loadConfig("INIT");
        LogHelper.info("Invoking start() function");
        Launcher.modulesManager.postInitModules();
        engine.start((String[]) args);
        Launcher.modulesManager.finishModules();
    }

    @Override
    public void preLoad() throws IOException {
        if (!isPreLoaded) {
            //loadConfig("API");
			//loadConfig("SETTINGS");
            loadConfig("CONFIG");
            isPreLoaded = true;
        }
    }

    @Override
    public void init(boolean clientInstance) {
        
    }
}
