package pro.gravit.launcher.sgui.config;

import java.io.IOException;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.NewLauncherSettings;
import pro.gravit.launcher.managers.SettingsManager;
import pro.gravit.launcher.sgui.api.GuiEngine;
import pro.gravit.launcher.sgui.api.GuiEngineConfig;
import pro.gravit.utils.helper.LogHelper;

public class SETTINGS extends GuiEngineConfig {
	public void main(GuiEngine engine) throws IOException {
		class settingsManagerClass extends SettingsManager {
			public NewLauncherSettings getDefaultConfig() {
				NewLauncherSettings new_settings = new NewLauncherSettings();
				new_settings.login = null;
				new_settings.rsaPassword = null;
				new_settings.profile = 0;
				
				new_settings.updatesDir = DirBridge.defaultUpdatesDir;
				new_settings.autoEnter = (boolean) engine.configget("autoEnterDefault", true);
				new_settings.fullScreen = (boolean) engine.configget("fullScreenDefault", true);
				new_settings.ram = (int) engine.configget("ramDefault", true);
				new_settings.lastDigest = null;
				new_settings.lastProfiles.clear();
				new_settings.lastHDirs.clear();
				return new_settings;
			}
		}
		settingsManagerClass settingsManager = new settingsManagerClass();
		engine.configset("SettingsManager", settingsManager.settings);
		engine.configset("SettingsManagerClass", settingsManager);
	}
}