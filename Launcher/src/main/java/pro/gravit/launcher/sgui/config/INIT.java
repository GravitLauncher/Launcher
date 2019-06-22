package pro.gravit.launcher.sgui.config;

import java.io.IOException;
import pro.gravit.launcher.managers.SettingsManager;
import pro.gravit.launcher.sgui.api.GuiEngine;
import pro.gravit.launcher.sgui.api.GuiEngineConfig;
import pro.gravit.utils.helper.LogHelper;

public class INIT extends GuiEngineConfig {
	private SettingsManager settingsManagerClass;
	public void main(GuiEngine engine) throws IOException {
		engine.config(engine, "SETTINGS");
		SettingsManager settingsManager = (SettingsManager) engine.configget("SettingsManagerClass",true);
		engine.configset("version-gui","0.0.1");
		settingsManager.loadConfig();
        engine.configset("SettingsManager", SettingsManager.settings);
        settingsManager.loadHDirStore();
		LogHelper.info("Инициализация...");
	}
}