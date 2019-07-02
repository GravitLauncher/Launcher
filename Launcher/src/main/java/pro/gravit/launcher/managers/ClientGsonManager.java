package pro.gravit.launcher.managers;

import com.google.gson.GsonBuilder;

import pro.gravit.launcher.client.UserSettings;
import pro.gravit.launcher.hwid.HWID;
import pro.gravit.launcher.hwid.HWIDProvider;
import pro.gravit.utils.UniversalJsonAdapter;

public class ClientGsonManager extends GsonManager {
    @Override
    public void registerAdapters(GsonBuilder builder) {
        super.registerAdapters(builder);
        builder.registerTypeAdapter(UserSettings.class, new UniversalJsonAdapter<>(UserSettings.providers));
        builder.registerTypeAdapter(HWID.class, new UniversalJsonAdapter<>(HWIDProvider.hwids));
    }
}
