package pro.gravit.launcher.modules.events;

import com.google.gson.GsonBuilder;
import pro.gravit.launcher.modules.LauncherModule;

public class PreGsonPhase extends LauncherModule.Event {
    public GsonBuilder gsonBuilder;
    public GsonBuilder gsonConfigBuilder;

    public PreGsonPhase(GsonBuilder gsonBuilder, GsonBuilder gsonConfigBuilder) {
        this.gsonBuilder = gsonBuilder;
        this.gsonConfigBuilder = gsonConfigBuilder;
    }
}
