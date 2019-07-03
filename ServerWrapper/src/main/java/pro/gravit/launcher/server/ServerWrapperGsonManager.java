package pro.gravit.launcher.server;

import com.google.gson.GsonBuilder;

import pro.gravit.launcher.managers.GsonManager;
import pro.gravit.launcher.request.websockets.ClientWebSocketService;

public class ServerWrapperGsonManager extends GsonManager {
    @Override
    public void registerAdapters(GsonBuilder builder) {
        super.registerAdapters(builder);
        ClientWebSocketService.appendTypeAdapters(builder);
    }
}
