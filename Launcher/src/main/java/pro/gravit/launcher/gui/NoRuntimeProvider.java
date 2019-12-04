package pro.gravit.launcher.gui;

import javax.swing.*;

public class NoRuntimeProvider implements RuntimeProvider {
    @Override
    public void run(String[] args) throws Exception {
        JOptionPane.showMessageDialog(null, "GUI часть лаунчера не найдена.\nС 5.1.0 вам необходимо самостоятельно установить модуль, отвечающий за GUI. Рантайм на JS более не поддерживается");
    }

    @Override
    public void preLoad() throws Exception {

    }

    @Override
    public void init(boolean clientInstance) {

    }
}
