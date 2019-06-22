package pro.gravit.launcher.sgui.config;

import java.io.IOException;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.sgui.api.GuiEngine;
import pro.gravit.launcher.sgui.api.GuiEngineConfig;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class CONFIG extends GuiEngineConfig {
	public void main(GuiEngine engine) throws IOException {
        engine.configset("dir", "GravitLauncher");
		engine.configset("title", "GravitLauncher");
		engine.configset("favicon", "favicon.png");
		engine.configset("linkText", "GravitLauncher");
		engine.configset("linkURL", new java.net.URL("https://gravitlauncher.ml"));
		engine.configset("discord", new java.net.URL("https://discord.gg/aJK6nMN"));
		engine.configset("settingsMagic", "0xC0DE5");
		engine.configset("autoEnterDefault", false);
		engine.configset("fullScreenDefault", false);
		engine.configset("ramDefault", 1024);
		DirBridge.dir = DirBridge.getLauncherDir(engine.configget("dir"));
		DirBridge.dirStore = DirBridge.getStoreDir(engine.configget("dir"));
		DirBridge.dirProjectStore = DirBridge.getProjectStoreDir(engine.configget("dir"));
		if (!IOHelper.isDir(DirBridge.dir)) {
			java.nio.file.Files.createDirectory(DirBridge.dir);
			}
			DirBridge.defaultUpdatesDir = DirBridge.dir.resolve("updates");
			if (!IOHelper.isDir(DirBridge.defaultUpdatesDir)) {
				java.nio.file.Files.createDirectory(DirBridge.defaultUpdatesDir);
				}
		LogHelper.info("Настройка...");
	}
}