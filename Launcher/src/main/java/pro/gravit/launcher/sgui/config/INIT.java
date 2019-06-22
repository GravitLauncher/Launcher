package pro.gravit.launcher.sgui.config;

import java.io.IOException;
import pro.gravit.launcher.managers.SettingsManager;
import pro.gravit.launcher.sgui.api.GuiEngine;
import pro.gravit.launcher.sgui.api.GuiEngineConfig;

public class INIT extends GuiEngineConfig {
	private SettingsManager settingsManager;
	void main(GuiEngine engine) throws IOException {
		engine.configset("version-gui","0.0.1");
		settingsManager.loadConfig();
        engine.configset("SettingsManager", settingsManager.settings);
        settingsManager.loadHDirStore();
		System.out.println("Инициализация...");
	}
}