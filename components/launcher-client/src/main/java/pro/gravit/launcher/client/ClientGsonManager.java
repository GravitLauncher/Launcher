package pro.gravit.launcher.client;

import com.google.gson.GsonBuilder;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.core.managers.GsonManager;
import pro.gravit.launcher.base.modules.events.PreGsonPhase;

public class ClientGsonManager extends GsonManager {
    private final ClientModuleManager moduleManager;

    public ClientGsonManager(ClientModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @Override
    public void registerAdapters(GsonBuilder builder) {
        super.registerAdapters(builder);
        builder.registerTypeAdapter(ClientProfile.Version.class, new ClientProfile.Version.GsonSerializer());
        moduleManager.invokeEvent(new PreGsonPhase(builder));
    }
}
