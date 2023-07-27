package pro.gravit.launcher.client;

import com.google.gson.GsonBuilder;
import pro.gravit.launcher.managers.GsonManager;
import pro.gravit.launcher.modules.events.PreGsonPhase;
import pro.gravit.launcher.request.websockets.ClientWebSocketService;
import pro.gravit.utils.UniversalJsonAdapter;

public class RuntimeGsonManager extends GsonManager {
    private final RuntimeModuleManager moduleManager;

    public RuntimeGsonManager(RuntimeModuleManager moduleManager) {
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
