package pro.gravit.launcher;

import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Application;

public abstract class JSApplication extends Application {
    private static final AtomicReference<JSApplication> INSTANCE = new AtomicReference<>();

    @LauncherAPI
    public static JSApplication getInstance() {
        return INSTANCE.get();
    }

    public JSApplication() {
        INSTANCE.set(this);
    }
}
