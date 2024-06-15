package pro.gravit.launcher.runtime.backend;

import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.core.api.LauncherAPIHolder;
import pro.gravit.launcher.core.api.features.AuthFeatureAPI;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.core.api.method.AuthMethod;
import pro.gravit.launcher.core.api.method.AuthMethodPassword;
import pro.gravit.launcher.core.api.model.SelfUser;
import pro.gravit.launcher.core.api.model.Texture;
import pro.gravit.launcher.core.api.model.UserPermissions;
import pro.gravit.launcher.core.backend.LauncherBackendAPI;
import pro.gravit.launcher.core.backend.UserSettings;
import pro.gravit.launcher.core.backend.exceptions.LauncherBackendException;
import pro.gravit.launcher.core.backend.extensions.Extension;
import pro.gravit.launcher.runtime.NewLauncherSettings;
import pro.gravit.launcher.runtime.managers.SettingsManager;
import pro.gravit.launcher.runtime.utils.LauncherUpdater;
import pro.gravit.utils.helper.JavaHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class LauncherBackendImpl implements LauncherBackendAPI {
    private final ClientDownloadImpl clientDownloadImpl = new ClientDownloadImpl(this);
    private volatile MainCallback callback;
    ExecutorService executorService;
    private volatile AuthMethod authMethod;
    // Settings
    private SettingsManager settingsManager;
    private NewLauncherSettings allSettings;
    private BackendSettings backendSettings;
    // Data
    private volatile List<ProfileFeatureAPI.ClientProfile> profiles;
    private volatile UserPermissions permissions;
    private volatile SelfUser selfUser;
    private volatile List<Java> availableJavas;
    private volatile CompletableFuture<List<Java>> availableJavasFuture;

    @Override
    public void setCallback(MainCallback callback) {
        this.callback = callback;
    }

    private void doInit() throws Exception {
        executorService = Executors.newScheduledThreadPool(2, (r) -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
        registerUserSettings("backend", BackendSettings.class);
        settingsManager = new SettingsManager();
        settingsManager.generateConfigIfNotExists();
        settingsManager.loadConfig();
        allSettings = settingsManager.getConfig();
        backendSettings = (BackendSettings) getUserSettings("backend", (k) -> new BackendSettings());
        permissions = new ClientPermissions();
    }

    @Override
    public CompletableFuture<LauncherInitData> init() {
        try {
            doInit();
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
        return LauncherAPIHolder.core().checkUpdates().thenCombineAsync(LauncherAPIHolder.core().getAuthMethods(), (updatesInfo, authMethods) -> {
            if(updatesInfo.required()) {
                try {
                    LauncherUpdater.prepareUpdate(URI.create(updatesInfo.url()).toURL());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                callback.onShutdown();
                LauncherUpdater.restart();
            }
            return new LauncherInitData(authMethods);
        }, executorService);
    }

    @Override
    public void selectAuthMethod(AuthMethod method) {
        this.authMethod = method;
        LauncherAPIHolder.changeAuthId(method.getName());
    }

    @Override
    public CompletableFuture<SelfUser> tryAuthorize() {
        if(this.authMethod == null) {
            return CompletableFuture.failedFuture(new LauncherBackendException("This method call not allowed before select authMethod"));
        }
        if(backendSettings.auth == null) {
            return CompletableFuture.failedFuture(new LauncherBackendException("Auth data not found"));
        }
        if(backendSettings.auth.expireIn > 0 && LocalDateTime.ofInstant(Instant.ofEpochMilli(backendSettings.auth.expireIn), ZoneOffset.UTC).isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            return LauncherAPIHolder.auth().refreshToken(backendSettings.auth.refreshToken).thenCompose((response) -> {
                setAuthToken(response);
                return LauncherAPIHolder.auth().restore(backendSettings.auth.accessToken, true);
            }).thenApply((user) -> {
                onAuthorize(user);
                return user;
            });
        }
        return LauncherAPIHolder.auth().restore(backendSettings.auth.accessToken, true).thenApply((user) -> {
            onAuthorize(user);
            return user;
        });
    }

    private void setAuthToken(AuthFeatureAPI.AuthToken authToken) {
        backendSettings.auth = new BackendSettings.AuthorizationData();
        backendSettings.auth.accessToken = authToken.getAccessToken();
        backendSettings.auth.refreshToken = authToken.getRefreshToken();
        if(authToken.getExpire() <= 0) {
            backendSettings.auth.expireIn = 0;
        }
        backendSettings.auth.expireIn = LocalDateTime.now(ZoneOffset.UTC)
                .plus(authToken.getExpire(), ChronoUnit.MILLIS).
                toEpochSecond(ZoneOffset.UTC);
    }

    private void onAuthorize(SelfUser selfUser) {
        permissions = selfUser.getPermissions();
        callback.onAuthorize(selfUser);
    }

    @Override
    public CompletableFuture<SelfUser> authorize(String login, AuthMethodPassword password) {
        if(this.authMethod == null) {
            return CompletableFuture.failedFuture(new LauncherBackendException("This method call not allowed before select authMethod"));
        }
        return LauncherAPIHolder.auth().auth(login, password).thenApply((response) -> {
            setAuthToken(response.authToken());
            onAuthorize(response.user());
            return response.user();
        });
    }

    @Override
    public CompletableFuture<List<ProfileFeatureAPI.ClientProfile>> fetchProfiles() {
        return LauncherAPIHolder.profile().getProfiles().thenApply((profiles) -> {
            this.profiles = profiles;
            callback.onProfiles(profiles);
            return profiles;
        });
    }

    @Override
    public ClientProfileSettings makeClientProfileSettings(ProfileFeatureAPI.ClientProfile profile) {
        var settings = backendSettings.settings.get(profile.getUUID());
        if(settings == null) {
            settings = new ProfileSettingsImpl((ClientProfile) profile);
        } else {
            settings = settings.copy();
            settings.initAfterGson((ClientProfile) profile, this);
        }
        return settings;
    }

    @Override
    public void saveClientProfileSettings(ClientProfileSettings settings) {
        var impl = (ProfileSettingsImpl) settings;
        impl.updateEnabledMods();
        backendSettings.settings.put(impl.profile.getUUID(), impl);
    }

    @Override
    public CompletableFuture<ReadyProfile> downloadProfile(ProfileFeatureAPI.ClientProfile profile, ClientProfileSettings settings, DownloadCallback callback) {
        return clientDownloadImpl.downloadProfile((ClientProfile) profile, (ProfileSettingsImpl) settings, callback);
    }

    @Override
    public CompletableFuture<byte[]> fetchTexture(Texture texture) {
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public CompletableFuture<List<Java>> getAvailableJava() {
        if(availableJavas == null) {
            if(availableJavasFuture == null) {
                availableJavasFuture = CompletableFuture.supplyAsync(() -> {
                    return (List) JavaHelper.findJava(); // TODO: Custom Java
                }, executorService).thenApply(e -> {
                    availableJavas = e;
                    return e;
                });
            }
        }
        return CompletableFuture.completedFuture(availableJavas);
    }

    @Override
    public void registerUserSettings(String name, Class<? extends UserSettings> clazz) {
        UserSettings.providers.register(name, clazz);
    }

    @Override
    public UserSettings getUserSettings(String name, Function<String, UserSettings> ifNotExist) {
        return allSettings.userSettings.computeIfAbsent(name, ifNotExist);
    }

    @Override
    public UserPermissions getPermissions() {
        return permissions;
    }

    @Override
    public boolean hasPermission(String permission) {
        return permissions.hasPerm(permission);
    }

    @Override
    public String getUsername() {
        return selfUser == null ? "Player" : getUsername();
    }

    @Override
    public SelfUser getSelfUser() {
        return selfUser;
    }

    @Override
    public <T extends Extension> T getExtension(Class<T> clazz) {
        return null;
    }

    @Override
    public void shutdown() {
        executorService.shutdownNow();
        try {
            settingsManager.saveConfig();
        } catch (IOException e) {
            LogHelper.error("Config not saved", e);
        }
    }
}
