package pro.gravit.launchserver.manangers;

import com.google.gson.GsonBuilder;
import pro.gravit.launcher.core.managers.GsonManager;
import pro.gravit.launcher.base.modules.events.PreGsonPhase;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.base.profiles.optional.triggers.OptionalTrigger;
import pro.gravit.launchserver.profiles.ProfilesProvider;
import pro.gravit.launchserver.distribution.UpdatesProvider;
import pro.gravit.launchserver.components.Component;
import pro.gravit.launchserver.modules.impl.LaunchServerModulesManager;
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
        builder.registerTypeAdapter(Component.class, new UniversalJsonAdapter<>(Component.providers));
        builder.registerTypeAdapter(OptionalAction.class, new UniversalJsonAdapter<>(OptionalAction.providers));
        builder.registerTypeAdapter(OptionalTrigger.class, new UniversalJsonAdapter<>(OptionalTrigger.providers));
        builder.registerTypeAdapter(ProfilesProvider.class, new UniversalJsonAdapter<>(ProfilesProvider.providers));
        builder.registerTypeAdapter(UpdatesProvider.class, new UniversalJsonAdapter<>(UpdatesProvider.providers));
        modulesManager.invokeEvent(new PreGsonPhase(builder));
        //ClientWebSocketService.appendTypeAdapters(builder);
    }
}
