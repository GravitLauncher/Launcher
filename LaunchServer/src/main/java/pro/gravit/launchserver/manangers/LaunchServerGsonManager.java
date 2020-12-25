package pro.gravit.launchserver.manangers;

import com.google.gson.GsonBuilder;
import pro.gravit.launcher.managers.GsonManager;
import pro.gravit.launcher.modules.events.PreGsonPhase;
import pro.gravit.launcher.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.request.JsonResultSerializeAdapter;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launchserver.auth.handler.AuthHandler;
import pro.gravit.launchserver.auth.protect.ProtectHandler;
import pro.gravit.launchserver.auth.protect.hwid.HWIDProvider;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.session.SessionStorage;
import pro.gravit.launchserver.auth.texture.TextureProvider;
import pro.gravit.launchserver.components.Component;
import pro.gravit.launchserver.dao.provider.DaoProvider;
import pro.gravit.launchserver.modules.impl.LaunchServerModulesManager;
import pro.gravit.launchserver.socket.WebSocketService;
import pro.gravit.launchserver.socket.response.UnknownResponse;
import pro.gravit.launchserver.socket.response.WebSocketServerResponse;
import pro.gravit.utils.UniversalJsonAdapter;

public class LaunchServerGsonManager extends GsonManager {
    private final LaunchServerModulesManager modulesManager;

    public LaunchServerGsonManager(LaunchServerModulesManager modulesManager) {
        this.modulesManager = modulesManager;
    }

    @Override
    public void registerAdapters(GsonBuilder builder) {
        super.registerAdapters(builder);
        builder.registerTypeAdapter(AuthProvider.class, new UniversalJsonAdapter<>(AuthProvider.providers));
        builder.registerTypeAdapter(TextureProvider.class, new UniversalJsonAdapter<>(TextureProvider.providers));
        builder.registerTypeAdapter(AuthHandler.class, new UniversalJsonAdapter<>(AuthHandler.providers));
        builder.registerTypeAdapter(Component.class, new UniversalJsonAdapter<>(Component.providers));
        builder.registerTypeAdapter(ProtectHandler.class, new UniversalJsonAdapter<>(ProtectHandler.providers));
        builder.registerTypeAdapter(DaoProvider.class, new UniversalJsonAdapter<>(DaoProvider.providers));
        builder.registerTypeAdapter(WebSocketServerResponse.class, new UniversalJsonAdapter<>(WebSocketService.providers, UnknownResponse.class));
        builder.registerTypeAdapter(WebSocketEvent.class, new JsonResultSerializeAdapter());
        builder.registerTypeAdapter(AuthRequest.AuthPasswordInterface.class, new UniversalJsonAdapter<>(AuthRequest.providers));
        builder.registerTypeAdapter(HWIDProvider.class, new UniversalJsonAdapter<>(HWIDProvider.providers));
        builder.registerTypeAdapter(OptionalAction.class, new UniversalJsonAdapter<>(OptionalAction.providers));
        builder.registerTypeAdapter(SessionStorage.class, new UniversalJsonAdapter<>(SessionStorage.providers));
        modulesManager.invokeEvent(new PreGsonPhase(builder));
        //ClientWebSocketService.appendTypeAdapters(builder);
    }
}
