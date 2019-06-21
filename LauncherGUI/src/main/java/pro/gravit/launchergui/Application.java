package pro.gravit.launchergui;

//import java.util.Map;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JLabel;
import pro.gravit.launchergui.helper.initGui;
public class Application {
	public static void createGUI(boolean build) {
		if (build) {
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
				createGUI(false);
			}
		}
		);
	}
}