package ru.gravit.launcher.managers;

import com.google.gson.GsonBuilder;

public class ClientGsonManager extends GsonManager {
    @Override
    public void registerAdapters(GsonBuilder builder) {
        super.registerAdapters(builder);

    }
}
