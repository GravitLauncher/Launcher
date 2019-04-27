package ru.gravit.launchserver.manangers;

import com.google.gson.GsonBuilder;
import ru.gravit.launcher.managers.GsonManager;
import ru.gravit.launchserver.auth.handler.AuthHandler;
import ru.gravit.launchserver.auth.hwid.HWIDHandler;
import ru.gravit.launchserver.auth.permissions.PermissionsHandler;
import ru.gravit.launchserver.auth.protect.ProtectHandler;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.auth.texture.TextureProvider;
import ru.gravit.launchserver.components.Component;
import ru.gravit.utils.UniversalJsonAdapter;

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
    }
}
