package pro.gravit.launchserver.profiles;

import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.ProviderMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class ProfilesProvider {
    public static final ProviderMap<ProfilesProvider> providers = new ProviderMap<>("ProfileProvider");
    private static boolean registredProviders = false;
    protected transient LaunchServer server;

    public static void registerProviders() {
        if (!registredProviders) {
            providers.register("local", LocalProfilesProvider.class);
            providers.register("remote", RemoteProfilesProvider.class);
            registredProviders = true;
        }
    }

    public void init(LaunchServer server) {
        this.server = server;
    }

    public abstract UncompletedProfile create(String name, String description, CompletedProfile basic);
    public abstract void delete(UncompletedProfile profile);
    public abstract Set<UncompletedProfile> getProfiles();
    public abstract CompletedProfile pushUpdate(UncompletedProfile profile,
                                    String tag,
                                    ClientProfile clientProfile,
                                    List<ProfileAction> assetActions,
                                    List<ProfileAction> clientActions,
                                    List<UpdateFlag> flags) throws IOException;
    public abstract void download(CompletedProfile profile, Map<String, Path> files, boolean assets) throws IOException;
    public abstract HashedDir getUnconnectedDirectory(String name);
    public abstract CompletedProfile get(UUID uuid, String tag);
    public abstract CompletedProfile get(String name, String tag);

    /** Actions and revision forms accepted by this provider. */
    public Capabilities capabilities() {
        return new Capabilities(EnumSet.allOf(ProfileAction.ProfileActionType.class), true);
    }

    protected final void requireSupportedRevision(String tag) {
        if (tag != null && !tag.isBlank() && !capabilities().namedRevisions() && !tag.equals("main")) {
            throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support named profile revisions: " + tag);
        }
    }

    protected final void requireSupportedActions(List<ProfileAction> actions) {
        if (actions == null) return;
        for (ProfileAction action : actions) {
            if (action == null) throw new IllegalArgumentException("Profile actions must not contain null entries");
            if (!capabilities().actions().contains(action.type())) {
                throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support profile action " + action.type());
            }
        }
    }

    public CompletedProfile get(UncompletedProfile profile) {
        if(profile == null) {
            return null;
        }
        return get(profile.getUuid(), null);
    }

    public void close() {

    }

    public interface UncompletedProfile {
        UUID getUuid();
        String getName();
        String getDescription();
        String getDefaultTag();
    }

    public interface CompletedProfile extends UncompletedProfile {
        String getTag();
        ClientProfile getProfile();
        HashedDir getClientDir();
        HashedDir getAssetDir();
    }

    public record ProfileAction(ProfileActionType type, String source, String target, Supplier<InputStream> input, Consumer<OutputStream> output, boolean deleteSource) {
        public enum ProfileActionType {
            UPLOAD, COPY, MOVE, DELETE
        }
        public static ProfileAction upload(Path source, String target, boolean deleteSource) {
            return new ProfileAction(ProfileActionType.UPLOAD, source.toString(), target, null, null, deleteSource);
        }

        public static ProfileAction upload(Supplier<InputStream> input, String target) {
            return new ProfileAction(ProfileActionType.UPLOAD, null, target, input, null, false);
        }
    }

    public enum UpdateFlag {
        USE_DEFAULT_ASSETS, DELETE_SOURCE_FILES
    }

    public record Capabilities(Set<ProfileAction.ProfileActionType> actions, boolean namedRevisions) {
        public Capabilities {
            actions = actions.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(actions));
        }
    }
}
