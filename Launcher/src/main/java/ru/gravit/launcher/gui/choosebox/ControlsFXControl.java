package ru.gravit.launcher.gui.choosebox;

import javafx.scene.control.Control;

abstract class ControlsFXControl extends Control {

    private String stylesheet;

    public ControlsFXControl() {

    }

    protected final String getUserAgentStylesheet(Class<?> clazz, String fileName) {

        if (stylesheet == null)
            stylesheet = clazz.getResource(fileName).toExternalForm();

        return stylesheet;
    }
}
