package pro.gravit.launcher.managers;

import com.google.gson.GsonBuilder;
import pro.gravit.launcher.client.ClientModuleManager;
import pro.gravit.launcher.client.UserSettings;
import pro.gravit.launcher.modules.events.PreGsonPhase;
import pro.gravit.launcher.request.websockets.ClientWebSocketService;
import pro.gravit.utils.UniversalJsonAdapter;

public class ClientGsonManager extends GsonManager {
    private final ClientModuleManager moduleManager;

    public ClientGsonManager(ClientModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @Override
    public void registerAdapters(GsonBuilder builder) {
        super.registerAdapters(builder);
        builder.registerTypeAdapter(UserSettings.class, new UniversalJsonAdapter<>(UserSettings.providers));
        ClientWebSocketService.appendTypeAdapters(builder);
        moduleManager.invokeEvent(new PreGsonPhase(builder));
    }
}
