package pro.gravit.launcher.sgui.api;

import javax.swing.SwingUtilities; 
import javax.swing.UIManager;

public class loading {
	public static void load(String frame) {
		try { 
    Class.forName("pro.gravit.launcher.sgui.gui."+frame);
}
catch (Exception e){
    System.out.println("Ошибка при загрузке "+frame);
}
	}
}