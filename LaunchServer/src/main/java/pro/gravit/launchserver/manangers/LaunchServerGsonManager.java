package pro.gravit.launchserver.manangers;

import com.google.gson.GsonBuilder;

import pro.gravit.launcher.hasher.HashedEntry;
import pro.gravit.launcher.hasher.HashedEntryAdapter;
import pro.gravit.launcher.hwid.HWID;
import pro.gravit.launcher.hwid.HWIDProvider;
import pro.gravit.launcher.managers.GsonManager;
import pro.gravit.launcher.request.JsonResultSerializeAdapter;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.launcher.request.websockets.ClientWebSocketService;
import pro.gravit.launchserver.auth.handler.AuthHandler;
import pro.gravit.launchserver.auth.hwid.HWIDHandler;
import pro.gravit.launchserver.auth.permissions.PermissionsHandler;
import pro.gravit.launchserver.auth.protect.ProtectHandler;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.texture.TextureProvider;
import pro.gravit.launchserver.components.Component;
import pro.gravit.launchserver.dao.provider.DaoProvider;
import pro.gravit.launchserver.socket.WebSocketService;
import pro.gravit.launchserver.socket.response.WebSocketServerResponse;
import pro.gravit.utils.UniversalJsonAdapter;

public class LaunchServerGsonManager extends GsonManager {
    @Override
    public void registerAdapters(GsonBuilder builder) {
        super.registerAdapters(builder);
        builder.registerTypeAdapter(AuthProvider.class, new UniversalJsonAdapter<>(AuthProvider.providers));
        builder.registerTypeAdapter(TextureProvider.class, new UniversalJsonAdapter<>(TextureProvider.providers));
        builder.registerTypeAdapter(AuthHandler.class, new UniversalJsonAdapter<>(AuthHandler.providers));
        builder.registerTypeAdapter(PermissionsHandler.class, new UniversalJsonAdapter<>(PermissionsHandler.providers));
        builder.registerTypeAdapter(HWIDHandler.class, new UniversalJsonAdapter<>(HWIDHandler.providers));
        builder.registerTypeAdapter(Component.class, new UniversalJsonAdapter<>(Component.providers));
        builder.registerTypeAdapter(ProtectHandler.class, new UniversalJsonAdapter<>(ProtectHandler.providers));
        builder.registerTypeAdapter(DaoProvider.class, new UniversalJsonAdapter<>(DaoProvider.providers));
        builder.registerTypeAdapter(HWID.class, new UniversalJsonAdapter<>(HWIDProvider.hwids));
        builder.registerTypeAdapter(WebSocketServerResponse.class, new UniversalJsonAdapter<>(WebSocketService.providers));
        builder.registerTypeAdapter(WebSocketEvent.class, new JsonResultSerializeAdapter());
        //ClientWebSocketService.appendTypeAdapters(builder);
    }
}
