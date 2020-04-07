package pro.gravit.launcher;

import javafx.application.Application;

import java.util.concurrent.atomic.AtomicReference;

public abstract class JSApplication extends Application {
    private static final AtomicReference<JSApplication> INSTANCE = new AtomicReference<>();


    public JSApplication() {
        INSTANCE.set(this);
    }

    public static JSApplication getInstance() {
        return INSTANCE.get();
    }
}
