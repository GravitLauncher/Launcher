package pro.gravit.launcher.modules.events;

import com.google.gson.GsonBuilder;
import pro.gravit.launcher.modules.LauncherModule;

public class PreGsonPhase extends LauncherModule.Event {
    public final GsonBuilder gsonBuilder;

    public PreGsonPhase(GsonBuilder gsonBuilder) {
        this.gsonBuilder = gsonBuilder;
    }
}
