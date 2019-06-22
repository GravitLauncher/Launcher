package pro.gravit.launcher.sgui;

import java.awt.Dimension;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JLabel;
import pro.gravit.launcher.sgui.helper.initGui;

public class Application {
	public static Map<String, String> Configs;
	public static Map<String, Object> Settings;
	public static void createGUI(Map<String, String> configs, Map<String, Object> settings, boolean build) {
		if (build) {
			Configs = configs;
			Settings = settings;
			initGui.init();
		} else {
		JFrame.setDefaultLookAndFeelDecorated(true);
		JFrame frame = new JFrame("Имя лаунчера не задано(Лаунчер не собран)");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JLabel label = new JLabel("Отсутствует конфиг, прошу соберите лаунчер.");
		frame.getContentPane().add(label);
		frame.setPreferredSize(new Dimension(500,100));
		frame.pack();
		frame.setVisible(true);
		}
	}
	public static void main(String... args) {
		javax.swing.SwingUtilities.invokeLater(
		new Runnable() {
			public void run() {
				createGUI(null, null, false);
			}
		}
		);
	}
}