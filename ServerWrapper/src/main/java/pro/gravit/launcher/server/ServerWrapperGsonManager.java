package pro.gravit.launcher.server;

import com.google.gson.GsonBuilder;
import pro.gravit.launcher.managers.GsonManager;
import pro.gravit.launcher.modules.events.PreGsonPhase;
import pro.gravit.launcher.request.websockets.ClientWebSocketService;

public class ServerWrapperGsonManager extends GsonManager {
    private final ServerWrapperModulesManager modulesManager;

    public ServerWrapperGsonManager(ServerWrapperModulesManager modulesManager) {
        this.modulesManager = modulesManager;
    }

    @Override
    public void registerAdapters(GsonBuilder builder) {
        super.registerAdapters(builder);
        ClientWebSocketService.appendTypeAdapters(builder);
        modulesManager.invokeEvent(new PreGsonPhase(builder));
    }
}
