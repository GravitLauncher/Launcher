package pro.gravit.launcher.server;

import com.google.gson.GsonBuilder;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.core.managers.GsonManager;

public class ServerWrapperGsonManager extends GsonManager {

    public ServerWrapperGsonManager() {
    }

    @Override
    public void registerAdapters(GsonBuilder builder) {
        super.registerAdapters(builder);
        builder.registerTypeAdapter(ClientProfile.Version.class, new ClientProfile.Version.GsonSerializer());
    }
}
