package pro.gravit.launcher.core;

import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.core.api.method.AuthMethod;
import pro.gravit.launcher.core.api.method.AuthMethodPassword;
import pro.gravit.launcher.core.api.model.SelfUser;
import pro.gravit.launcher.core.api.model.Texture;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface LauncherBackendAPI {
    void setCallback(MainCallback callback);
    CompletableFuture<LauncherInitData> init();
    void selectAuthMethod(AuthMethod method);
    CompletableFuture<SelfUser> tryAuthorize();
    CompletableFuture<SelfUser> authorize(String login, AuthMethodPassword password);
    CompletableFuture<List<ProfileFeatureAPI.ClientProfile>> fetchProfiles();
    ClientProfileSettings makeClientProfileSettings(ProfileFeatureAPI.ClientProfile profile);
    CompletableFuture<ReadyProfile> downloadProfile(ProfileFeatureAPI.ClientProfile profile, ClientProfileSettings settings, DownloadCallback callback);
    // Tools
    CompletableFuture<byte[]> fetchTexture(Texture texture);

    record LauncherInitData(List<AuthMethod> methods) {}

    interface ReadyProfile {
        ProfileFeatureAPI.ClientProfile getClientProfile();
        ClientProfileSettings getSettings();
        void run(RunCallback callback) throws Exception;
    }

    interface ClientProfileSettings {
        long getReservedMemoryBytes();
        long getMaxMemoryBytes();
        void setReservedMemoryBytes(long value);
        List<Flag> getFlags();
        boolean hasFlag(Flag flag);
        void addFlag(Flag flag);
        void removeFlag(Flag flag);
        List<ProfileFeatureAPI.OptionalMod> getEnabledOptionals();
        void enableOptional(ProfileFeatureAPI.OptionalMod mod, ChangedOptionalStatusCallback callback);
        void disableOptional(ProfileFeatureAPI.OptionalMod mod, ChangedOptionalStatusCallback callback);

        enum Flag {

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
    }

    class RunCallback {
        public void onStarted() {

        }

        public void onFinished(int code) {

        }

        public void onNormalOutput(byte[] buf, int offset, int size) {

        }

        public void onErrorOutput(byte[] buf, int offset, int size) {

        }
    }

    class DownloadCallback {
        public void onStage(String stage) {

        }

        public void onCanCancel(Runnable cancel) {

        }

        public void onTotalDownload(long total) {

        }

        public void onCurrentDownloaded(long current) {

        }
    }
}
