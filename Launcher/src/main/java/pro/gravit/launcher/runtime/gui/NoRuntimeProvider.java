package pro.gravit.launcher.runtime.gui;

import javax.swing.*;

public class NoRuntimeProvider implements RuntimeProvider {
    @Override
    public void run(String[] args) {
        JOptionPane.showMessageDialog(null, "Модуль графического интерфейса лаунчера отсутствует");
    }

    @Override
    public void preLoad() {

    }

    @Override
    public void init(boolean clientInstance) {

    }
}
