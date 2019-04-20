package ru.gravit.launcher.managers;

import com.google.gson.GsonBuilder;
import ru.gravit.launcher.client.UserSettings;
import ru.gravit.utils.UniversalJsonAdapter;

public class ClientGsonManager extends GsonManager {
    @Override
    public void registerAdapters(GsonBuilder builder) {
        super.registerAdapters(builder);
        builder.registerTypeAdapter(UserSettings.class, new UniversalJsonAdapter<>(UserSettings.providers));
    }
}
