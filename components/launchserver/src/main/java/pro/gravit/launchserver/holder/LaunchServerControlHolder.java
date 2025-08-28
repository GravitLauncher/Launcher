package pro.gravit.launchserver.holder;

import pro.gravit.utils.launch.ClassLoaderControl;

public class LaunchServerControlHolder {
    private static ClassLoaderControl control;
    private static ModuleLayer.Controller controller;

    public static ClassLoaderControl getControl() {
        return control;
    }

    public static void setControl(ClassLoaderControl control) {
        LaunchServerControlHolder.control = control;
    }

    public static ModuleLayer.Controller getController() {
        return controller;
    }

    public static void setController(ModuleLayer.Controller controller) {
        LaunchServerControlHolder.controller = controller;
    }
}
