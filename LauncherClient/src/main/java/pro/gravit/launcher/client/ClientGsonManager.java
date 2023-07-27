package pro.gravit.launcher.client;

import com.google.gson.GsonBuilder;
import pro.gravit.launcher.managers.GsonManager;
import pro.gravit.launcher.modules.events.PreGsonPhase;
import pro.gravit.launcher.request.websockets.ClientWebSocketService;

public class ClientGsonManager extends GsonManager {
    private final ClientModuleManager moduleManager;

    public ClientGsonManager(ClientModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @Override
    public void registerAdapters(GsonBuilder builder) {
        super.registerAdapters(builder);
        ClientWebSocketService.appendTypeAdapters(builder);
        moduleManager.invokeEvent(new PreGsonPhase(builder));
    }
}
