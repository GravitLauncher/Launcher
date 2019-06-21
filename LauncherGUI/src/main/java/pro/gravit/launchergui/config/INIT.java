package pro.gravit.launchergui.config;

import pro.gravit.launchergui.api.GuiEngine;
import pro.gravit.launchergui.api.GuiEngineConfig;

public class INIT extends GuiEngineConfig {
	void main(GuiEngine engine) {
		engine.configset("version-gui","0.0.1");
	}
}