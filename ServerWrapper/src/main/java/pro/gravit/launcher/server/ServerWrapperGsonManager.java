package pro.gravit.launcher.server;

import com.google.gson.GsonBuilder;
import pro.gravit.launcher.core.managers.GsonManager;
import pro.gravit.launcher.base.request.websockets.ClientWebSocketService;

public class ServerWrapperGsonManager extends GsonManager {

    public ServerWrapperGsonManager() {
    }

    @Override
    public void registerAdapters(GsonBuilder builder) {
        super.registerAdapters(builder);
        ClientWebSocketService.appendTypeAdapters(builder);
    }
}
