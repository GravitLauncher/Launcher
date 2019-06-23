package pro.gravit.launcher.sgui.gui;

import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JPanel;
public class login {
	public static JButton loginbutton = new JButton("Войти");
    public static JTextField login = new JTextField("Логин");
	public static JPasswordField password = new JPasswordField("Пароль");
	public static JFrame frame = new JFrame("Имя лаунчера не задано(Лаунчер не собран)");
	static {
		JPanel contents = new JPanel();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        contents.add(login);
		login.setPreferredSize( new Dimension( 70, 25 ) );
		password.setPreferredSize( new Dimension( 70, 25 ) );
        contents.add(password);
        contents.add(loginbutton);
		frame.add(contents);
		frame.setPreferredSize(new Dimension(400,100));
		frame.pack();
		frame.setVisible(true);
		
	}
}