package pro.gravit.launcher.sgui.api;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import pro.gravit.launcher.sgui.Application;
import pro.gravit.utils.helper.LogHelper;
public class GuiEngine {
	private Map<String, String> Configs = new HashMap<String, String>();
	private Map<String, Object> Settings = new HashMap<String, Object>();
	public void start(String[] args) {
		Application.createGUI(Configs, Settings, true);
	}
	public void config(GuiEngine engine,String config) {
		try {
			LogHelper.info("Настройка "+config+" Загружена");
			Class c = Class.forName("pro.gravit.launcher.sgui.config."+config);
			Method runmethod = c.getMethod("main", GuiEngine.class);
			runmethod.invoke(c.newInstance(), engine);
	}
catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e){
    LogHelper.info("Ошибка при загрузке настройки "+config);
	e.printStackTrace();
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