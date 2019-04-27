package ru.gravit.launcher.server;

import com.google.gson.GsonBuilder;
import ru.gravit.launcher.managers.GsonManager;

public class ServerWrapperGsonManager extends GsonManager {
    @Override
    public void registerAdapters(GsonBuilder builder) {
        super.registerAdapters(builder);
    }
}
