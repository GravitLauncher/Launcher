package ru.gravit.launcher;

import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Application;
import ru.gravit.launcher.LauncherAPI;

@SuppressWarnings("AbstractClassNeverImplemented")
public abstract class JSApplication extends Application {
    private static final AtomicReference<JSApplication> INSTANCE = new AtomicReference<>();

    @LauncherAPI
    public static JSApplication getInstance() {
        return INSTANCE.get();
    }


    @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
    public JSApplication() {
        INSTANCE.set(this);
    }
}
