package pro.gravit.launcher.sgui.api;

import java.util.HashMap;
import java.util.Map;
import pro.gravit.launcher.sgui.Application;
public class GuiEngine {
	private Map<String, String> Configs = new HashMap<String, String>();
	private Map<String, Object> Settings = new HashMap<String, Object>();
	public void start(String[] args) {
		Application.createGUI(Configs, Settings, true);
	}
	public void config(GuiEngine engine,String config) {
		try {
			System.out.println("Настройка "+config+" Загружена");
			Class c = Class.forName("pro.gravit.launcher.sgui.config."+config);
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
	public void configset(String path, Object value) {
		
		Settings.put(path, value);
	}
	public Object configget(String path, boolean fool) {
		
		return Settings.get(path);
	}
	public String configget(String path) {
		
		return Configs.get(path);
	}
	public static GuiEngine getGuiEngine() {
		GuiEngine var = new GuiEngine();
		return var;
	}
	}