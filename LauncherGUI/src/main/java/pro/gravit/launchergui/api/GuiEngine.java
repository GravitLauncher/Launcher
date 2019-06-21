package pro.gravit.launchergui.api;

import java.util.HashMap;
import java.util.Map;
import pro.gravit.launchergui.Application;
public class GuiEngine {
	private Map<String, String> Configs = new HashMap<String, String>();
	public void start(String[] args) {
		Application.createGUI(true);
	}
	public void config(GuiEngine engine,String config) {
		try {
			System.out.println("Настройка "+config+" Загружена");
			Class c = Class.forName("pro.gravit.launchergui.config."+config);
			Object configclass = c.newInstance();
			GuiEngineConfig runclass = (GuiEngineConfig) configclass;
			runclass.main(engine);
			
	}
catch (Exception e){
    System.out.println("Ошибка при загрузке настройки "+config);
}
	}
	public void configset(String path, String value) {
		
		Configs.put(path, value);
	}
	public static GuiEngine getGuiEngine() {
		GuiEngine var = new GuiEngine();
		return var;
	}
	}