package pro.gravit.launcher.core.backend;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.core.api.method.AuthMethod;
import pro.gravit.launcher.core.api.method.AuthMethodPassword;
import pro.gravit.launcher.core.api.model.SelfUser;
import pro.gravit.launcher.core.api.model.Texture;
import pro.gravit.launcher.core.api.model.UserPermissions;
import pro.gravit.launcher.core.backend.extensions.Extension;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface LauncherBackendAPI {
    void setCallback(MainCallback callback);
    CompletableFuture<LauncherInitData> init();
    void selectAuthMethod(AuthMethod method);
    CompletableFuture<SelfUser> tryAuthorize();
    CompletableFuture<SelfUser> authorize(String login, AuthMethodPassword password);
    CompletableFuture<List<ProfileFeatureAPI.ClientProfile>> fetchProfiles();
    ClientProfileSettings makeClientProfileSettings(ProfileFeatureAPI.ClientProfile profile);
    void saveClientProfileSettings(ClientProfileSettings settings);
    CompletableFuture<ReadyProfile> downloadProfile(ProfileFeatureAPI.ClientProfile profile, ClientProfileSettings settings, DownloadCallback callback);
    // Tools
    CompletableFuture<byte[]> fetchTexture(Texture texture);
    CompletableFuture<List<Java>> getAvailableJava();
    CompletableFuture<ServerPingInfo> pingServer(ProfileFeatureAPI.ClientProfile profile);
    // Settings
    void registerUserSettings(String name, Class<? extends UserSettings> clazz);
    UserSettings getUserSettings(String name, Function<String, UserSettings> ifNotExist);
    // Status
    UserPermissions getPermissions();
    boolean hasPermission(String permission);
    String getUsername();
    SelfUser getSelfUser();
    boolean isTestMode();
    // Extensions
    <T extends Extension> T getExtension(Class<T> clazz);
    void shutdown();

    record LauncherInitData(List<AuthMethod> methods) {}

    interface ReadyProfile {
        ProfileFeatureAPI.ClientProfile getClientProfile();
        ClientProfileSettings getSettings();
        void run(RunCallback callback) throws Exception;
    }

    interface ClientProfileSettings {
        long getReservedMemoryBytes(MemoryClass memoryClass);
        long getMaxMemoryBytes(MemoryClass memoryClass);
        void setReservedMemoryBytes(MemoryClass memoryClass, long value);
        Set<Flag> getFlags();
        Set<Flag> getAvailableFlags();
        boolean hasFlag(Flag flag);
        void addFlag(Flag flag);
        void removeFlag(Flag flag);
        Set<ProfileFeatureAPI.OptionalMod> getEnabledOptionals();
        void enableOptional(ProfileFeatureAPI.OptionalMod mod, ChangedOptionalStatusCallback callback);
        void disableOptional(ProfileFeatureAPI.OptionalMod mod, ChangedOptionalStatusCallback callback);
        Java getSelectedJava();
        Java getRecommendedJava();
        void setSelectedJava(Java java);
        boolean isRecommended(Java java);
        boolean isCompatible(Java java);
        ClientProfileSettings copy();

        enum Flag {
            @LauncherNetworkAPI
            AUTO_ENTER,
            @LauncherNetworkAPI
            FULLSCREEN,
            @LauncherNetworkAPI
            LINUX_WAYLAND_SUPPORT,
            @LauncherNetworkAPI
            DEBUG_SKIP_FILE_MONITOR
        }

        enum MemoryClass {
            TOTAL
        }

        interface ChangedOptionalStatusCallback {
            void onChanged(ProfileFeatureAPI.OptionalMod mod, boolean enabled);
        }
    }

    // Callbacks

    class MainCallback {
        // On any request
        public void onChangeStatus(String status) {

        }

        public void onProfiles(List<ProfileFeatureAPI.ClientProfile> profiles) {

        }

        public void onAuthorize(SelfUser selfUser) {

        }

        public void onNotify(String header, String description) {

        }

        public void onShutdown() {

        }
    }

    class RunCallback {
        public void onStarted() {

        }

        public void onCanTerminate(Runnable terminate) {

        }

        public void onFinished(int code) {

        }

        public void onNormalOutput(byte[] buf, int offset, int size) {

        }

        public void onErrorOutput(byte[] buf, int offset, int size) {

        }
    }

    class DownloadCallback {
        public static final String STAGE_ASSET_VERIFY = "assetVerify";
        public static final String STAGE_HASHING = "hashing";
        public static final String STAGE_DIFF = "diff";
        public static final String STAGE_DOWNLOAD = "download";
        public static final String STAGE_DELETE_EXTRA = "deleteExtra";
        public static final String STAGE_DONE_PART = "done.part";
        public static final String STAGE_DONE = "done";

        public void onStage(String stage) {

        }

        public void onCanCancel(Runnable cancel) {

        }

        public void onTotalDownload(long total) {

        }

        public void onCurrentDownloaded(long current) {

        }
    }

    interface Java {
        int getMajorVersion();
        Path getPath();
    }

    interface ServerPingInfo {
        int getMaxOnline();
        int getOnline();
        List<String> getPlayerNames();
    }
}
