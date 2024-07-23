package pro.gravit.launcher.runtime.client;

import com.google.gson.GsonBuilder;
import pro.gravit.launcher.start.RuntimeModuleManager;
import pro.gravit.launcher.core.managers.GsonManager;
import pro.gravit.launcher.base.modules.events.PreGsonPhase;
import pro.gravit.launcher.base.request.websockets.ClientWebSocketService;
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
