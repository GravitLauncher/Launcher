package ru.gravit.launchserver.manangers;

import com.google.gson.GsonBuilder;
import ru.gravit.launcher.Launcher;
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
        Launcher.gsonManager.gsonBuilder = new GsonBuilder();
        Launcher.gsonManager.gsonBuilder.registerTypeAdapter(AuthProvider.class, new UniversalJsonAdapter<>(AuthProvider.providers));
        Launcher.gsonManager.gsonBuilder.registerTypeAdapter(TextureProvider.class, new UniversalJsonAdapter<>(TextureProvider.providers));
        Launcher.gsonManager.gsonBuilder.registerTypeAdapter(AuthHandler.class, new UniversalJsonAdapter<>(AuthHandler.providers));
        Launcher.gsonManager.gsonBuilder.registerTypeAdapter(PermissionsHandler.class, new UniversalJsonAdapter<>(PermissionsHandler.providers));
        Launcher.gsonManager.gsonBuilder.registerTypeAdapter(HWIDHandler.class, new UniversalJsonAdapter<>(HWIDHandler.providers));
        Launcher.gsonManager.gsonBuilder.registerTypeAdapter(Component.class, new UniversalJsonAdapter<>(Component.providers));
        Launcher.gsonManager.gsonBuilder.registerTypeAdapter(ProtectHandler.class, new UniversalJsonAdapter<>(ProtectHandler.providers));
    }
}
