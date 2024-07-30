package pro.gravit.launchserver.manangers;

import com.google.gson.GsonBuilder;
import pro.gravit.launcher.base.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.core.managers.GsonManager;
import pro.gravit.launcher.base.modules.events.PreGsonPhase;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.base.profiles.optional.triggers.OptionalTrigger;
import pro.gravit.launcher.base.request.JsonResultSerializeAdapter;
import pro.gravit.launcher.base.request.WebSocketEvent;
import pro.gravit.launcher.base.request.auth.AuthRequest;
import pro.gravit.launcher.base.request.auth.GetAvailabilityAuthRequest;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.mix.MixProvider;
import pro.gravit.launchserver.auth.password.PasswordVerifier;
import pro.gravit.launchserver.auth.profiles.ProfileProvider;
import pro.gravit.launchserver.auth.protect.ProtectHandler;
import pro.gravit.launchserver.auth.texture.TextureProvider;
import pro.gravit.launchserver.auth.updates.UpdatesProvider;
import pro.gravit.launchserver.components.Component;
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
        builder.registerTypeAdapter(ClientProfile.Version.class, new ClientProfile.Version.GsonSerializer());
        builder.registerTypeAdapter(TextureProvider.class, new UniversalJsonAdapter<>(TextureProvider.providers));
        builder.registerTypeAdapter(AuthCoreProvider.class, new UniversalJsonAdapter<>(AuthCoreProvider.providers));
        builder.registerTypeAdapter(PasswordVerifier.class, new UniversalJsonAdapter<>(PasswordVerifier.providers));
        builder.registerTypeAdapter(Component.class, new UniversalJsonAdapter<>(Component.providers));
        builder.registerTypeAdapter(ProtectHandler.class, new UniversalJsonAdapter<>(ProtectHandler.providers));
        builder.registerTypeAdapter(WebSocketServerResponse.class, new UniversalJsonAdapter<>(WebSocketService.providers, UnknownResponse.class));
        builder.registerTypeAdapter(WebSocketEvent.class, new JsonResultSerializeAdapter());
        builder.registerTypeAdapter(AuthRequest.AuthPasswordInterface.class, new UniversalJsonAdapter<>(AuthRequest.providers));
        builder.registerTypeAdapter(GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails.class, new UniversalJsonAdapter<>(GetAvailabilityAuthRequest.providers));
        builder.registerTypeAdapter(OptionalAction.class, new UniversalJsonAdapter<>(OptionalAction.providers));
        builder.registerTypeAdapter(OptionalTrigger.class, new UniversalJsonAdapter<>(OptionalTrigger.providers));
        builder.registerTypeAdapter(MixProvider.class, new UniversalJsonAdapter<>(MixProvider.providers));
        builder.registerTypeAdapter(ProfileProvider.class, new UniversalJsonAdapter<>(ProfileProvider.providers));
        builder.registerTypeAdapter(UpdatesProvider.class, new UniversalJsonAdapter<>(UpdatesProvider.providers));
        modulesManager.invokeEvent(new PreGsonPhase(builder));
        //ClientWebSocketService.appendTypeAdapters(builder);
    }
}
