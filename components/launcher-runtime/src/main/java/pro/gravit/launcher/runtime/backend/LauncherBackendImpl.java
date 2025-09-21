package pro.gravit.launcher.runtime.backend;

import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.vfs.Vfs;
import pro.gravit.launcher.base.vfs.directory.FileVfsDirectory;
import pro.gravit.launcher.base.vfs.file.CachedVfsFile;
import pro.gravit.launcher.base.vfs.file.UrlVfsFile;
import pro.gravit.launcher.core.api.LauncherAPIHolder;
import pro.gravit.launcher.core.api.features.*;
import pro.gravit.launcher.core.api.method.AuthMethod;
import pro.gravit.launcher.core.api.method.AuthMethodPassword;
import pro.gravit.launcher.core.api.model.SelfUser;
import pro.gravit.launcher.core.api.model.Texture;
import pro.gravit.launcher.core.api.model.UserPermissions;
import pro.gravit.launcher.core.backend.LauncherBackendAPI;
import pro.gravit.launcher.core.backend.UserSettings;
import pro.gravit.launcher.core.backend.exceptions.LauncherBackendException;
import pro.gravit.launcher.core.backend.extensions.Extension;
import pro.gravit.launcher.core.backend.extensions.TextureUploadExtension;
import pro.gravit.launcher.runtime.LauncherEngine;
import pro.gravit.launcher.runtime.NewLauncherSettings;
import pro.gravit.launcher.runtime.client.DirBridge;
import pro.gravit.launcher.runtime.client.ServerPinger;
import pro.gravit.launcher.runtime.debug.DebugMain;
import pro.gravit.launcher.runtime.managers.SettingsManager;
import pro.gravit.launcher.runtime.utils.HWIDProvider;
import pro.gravit.launcher.runtime.utils.LauncherUpdater;
import pro.gravit.utils.helper.*;

import java.io.*;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LauncherBackendImpl implements LauncherBackendAPI, TextureUploadExtension {
    private final ClientDownloadImpl clientDownloadImpl = new ClientDownloadImpl(this);
    private volatile MainCallback callback;
    ExecutorService executorService;
    private volatile AuthMethod authMethod;
    // Settings
    private SettingsManager settingsManager;
    private NewLauncherSettings allSettings;
    private BackendSettings backendSettings;
    // Hardware
    private volatile ECKeyHolder ecKeyHolder;
    // Data
    private volatile Map<UUID, ProfileFeatureAPI.ClientProfile> profiles;
    private volatile UserPermissions permissions;
    private volatile SelfUser selfUser;
    private volatile List<Java> availableJavas;
    private volatile CompletableFuture<List<Java>> availableJavasFuture;
    private volatile CompletableFuture<Void> processHardwareFuture;
    private volatile Path vfsRootPath;
    private final Map<UUID, CompletableFuture<ServerPingInfo>> pingFutures = new ConcurrentHashMap<>();

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
        ecKeyHolder = new ECKeyHolder();
        ecKeyHolder.readKeys();
        DirBridge.dirUpdates = DirBridge.defaultUpdatesDir;
    }

    @Override
    public CompletableFuture<LauncherInitData> init() {
        try {
            doInit();
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
        CompletableFuture<CoreFeatureAPI.LauncherUpdateInfo> feature;
        if(isTestMode()) {
            feature = CompletableFuture.completedFuture(new CoreFeatureAPI.LauncherUpdateInfo(null, "Unknown", false, false));
        } else {
            feature = LauncherAPIHolder.core().checkUpdates();
        }
        return feature.thenCombineAsync(LauncherAPIHolder.core().getAuthMethods(), (updatesInfo, authMethods) -> {
            if(updatesInfo.required()) {
                Path tempFile;
                try {
                    tempFile = LauncherUpdater.prepareUpdate(URI.create(updatesInfo.url()).toURL());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                callback.onShutdown();
                shutdown();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try(InputStream input = IOHelper.newInput(tempFile)) {
                        try(OutputStream output = IOHelper.newOutput(LauncherUpdater.getLauncherPath())) {
                            input.transferTo(output);
                        }
                        LauncherUpdater.restart();
                    } catch (IOException e) {
                        LogHelper.error(e);
                    }
                }));
            }
            return new LauncherInitData(authMethods);
        }, executorService);
    }

    public AuthFeatureAPI.AuthToken getAuthToken() {
        return backendSettings.auth.toToken();
    }

    public AuthMethod getAuthMethod() {
        return authMethod;
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
        this.selfUser = selfUser;
        permissions = selfUser.getPermissions();
        callback.onAuthorize(selfUser);
        if(processHardwareFuture == null) {
            processHardwareFuture = processHardware();
        }
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
            onProfiles(profiles);
            callback.onProfiles(profiles);
            return profiles;
        });
    }

    private void onProfiles(List<ProfileFeatureAPI.ClientProfile> profiles) {
        this.profiles = profiles.stream().collect(Collectors.toMap(ProfileFeatureAPI.ClientProfile::getUUID, x -> x));
        for(var e : this.profiles.values()) {
            if(e instanceof ClientProfile cp) {
                cp.updateOptionalGraph();
            }
        }
        for(var e : backendSettings.settings.entrySet()) {
            ClientProfile profile = (ClientProfile) this.profiles.get(e.getKey());
            if(profile == null) {
                continue;
            }
            e.getValue().initAfterGson(profile, this);
        }
    }

    @Override
    public ClientProfileSettings makeClientProfileSettings(ProfileFeatureAPI.ClientProfile profile) {
        var settings = backendSettings.settings.get(profile.getUUID());
        if(settings == null) {
            settings = new ProfileSettingsImpl((ClientProfile) profile);
            settings.backend = this;
            settings.updateEnabledMods();
        } else {
            if(settings.backend == null) {
                settings.initAfterGson((ClientProfile) profile, this);
            }
            settings = settings.copy();
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
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }

    @Override
    public CompletableFuture<List<Java>> getAvailableJava() {
        if(availableJavas == null) {
            if(availableJavasFuture == null) {
                availableJavasFuture = CompletableFuture.supplyAsync(() -> {
                    List<Java> javas = getCustomJava();
                    if(!Launcher.getConfig().forceUseCustomJava || javas.isEmpty()) {
                        javas.addAll(JavaHelper.findJava());
                    }
                    return javas;
                }, executorService).thenApply(e -> {
                    availableJavas = e;
                    return e;
                });
            }
            return availableJavasFuture;
        }
        return CompletableFuture.completedFuture(availableJavas);
    }

    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile(
            "Java (?<version>.+) b(?<build>.+) (?<os>.+) (?<arch>.+) javafx (?<javafx>.+)");

    public List<Java> getCustomJava() {
        List<Java> versions = new ArrayList<>();
        if(Launcher.getConfig().customJavaDownload == null) {
            return versions;
        }
        for (Map.Entry<String, String> entry : Launcher.getConfig().customJavaDownload.entrySet()) {
            String javaDir = entry.getKey();
            String javaVersionString = entry.getValue();
            Matcher matcher = JAVA_VERSION_PATTERN.matcher(javaVersionString);
            if (matcher.matches()) {
                String os = matcher.group("os");
                int version = Integer.parseInt(matcher.group("version"));
                int build = Integer.parseInt(matcher.group("build"));
                JVMHelper.ARCH arch = JVMHelper.ARCH.valueOf(matcher.group("arch"));
                boolean javafx = Boolean.parseBoolean(matcher.group("javafx"));
                if (!isArchAvailable(arch)) {
                    continue;
                }
                if (!JVMHelper.OS_TYPE.name.equals(os)) {
                    continue;
                }
                Path javaDirectory = DirBridge.dirUpdates.resolve(javaDir);
                LogHelper.debug("In-Launcher Java Version found: Java %d b%d %s javafx %s", version, build,
                        arch.name, Boolean.toString(javafx));
                JavaHelper.JavaVersion javaVersion = new JavaHelper.JavaVersion(javaDirectory, version, build,
                        arch, javafx);
                versions.add(javaVersion);
            } else {
                LogHelper.warning("Java Version: %s does not match", javaVersionString);
            }
        }
        return versions;
    }

    public boolean isArchAvailable(JVMHelper.ARCH arch) {
        if (JVMHelper.ARCH_TYPE == arch) {
            return true;
        }
        if (arch == JVMHelper.ARCH.X86_64 && JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE
                && ((JVMHelper.ARCH_TYPE == JVMHelper.ARCH.X86 && !JVMHelper.isJVMMatchesSystemArch())
                || JVMHelper.ARCH_TYPE == JVMHelper.ARCH.ARM64)) {
            return true;
        }
        return arch == JVMHelper.ARCH.X86_64
                && JVMHelper.OS_TYPE == JVMHelper.OS.MACOSX
                && JVMHelper.ARCH_TYPE == JVMHelper.ARCH.ARM64;
    }

    @Override
    public CompletableFuture<ServerPingInfo> pingServer(ProfileFeatureAPI.ClientProfile profile) {
        return pingFutures.computeIfAbsent(profile.getUUID(), (k) -> {
            CompletableFuture<ServerPingInfo> future = new CompletableFuture<>();
            executorService.submit(() -> {
                try {
                    ServerPinger pinger = new ServerPinger((ClientProfile) profile);
                    future.complete(pinger.ping());
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            });
            return future;
        });
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
    public boolean isTestMode() {
        try {
            return DebugMain.IS_DEBUG.get();
        } catch (Throwable ex) {
            return false;
        }
    }

    @Override
    public ResourceLayer makeResourceLayer(List<Path> overlayList) {
        if(vfsRootPath == null) {
            vfsRootPath = initVfsDirectory();
        }
        return new ResourceLayerImpl(vfsRootPath, overlayList);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Extension> T getExtension(Class<T> clazz) {
        if(clazz == TextureUploadExtension.class) {
            if(authMethod != null && authMethod.getFeatures().contains(TextureUploadFeatureAPI.FEATURE_NAME)) {
                return (T) this;
            }
        }
        return null;
    }

    @Override
    public void shutdown() {
        if(executorService != null) {
            executorService.shutdownNow();
        }
        if(settingsManager != null) {
            try {
                settingsManager.saveConfig();
            } catch (IOException e) {
                LogHelper.error("Config not saved", e);
            }
        }
    }

    @Override
    public CompletableFuture<TextureUploadFeatureAPI.TextureUploadInfo> fetchTextureUploadInfo() {
        return LauncherAPIHolder.get().get(TextureUploadFeatureAPI.class).fetchInfo();
    }

    @Override
    public CompletableFuture<Texture> uploadTexture(String name, byte[] bytes, TextureUploadFeatureAPI.UploadSettings settings) {
        return LauncherAPIHolder.get().get(TextureUploadFeatureAPI.class).upload(name, bytes, settings);
    }

    public CompletableFuture<Void> processHardware() {
        HardwareVerificationFeatureAPI featureAPI = LauncherAPIHolder.get().get(HardwareVerificationFeatureAPI.class);
        if(featureAPI == null) {
            return CompletableFuture.completedFuture(null);
        }
        return featureAPI.getSecurityInfo().thenCompose((response) -> {
            if(!response.isRequired()) {
                return CompletableFuture.completedFuture(null);
            }
            byte[] signature = SecurityHelper.sign(response.getSignData(), ecKeyHolder.privateKey);
            return featureAPI.privateKeyVerification(ecKeyHolder.publicKey, signature);
        }).thenCompose((response) -> {
            switch (response.getHardwareCollectLevel()) {
                case NONE -> {
                    return featureAPI.sendHardwareInfo(null, null);
                }
                case ONLY_STATISTIC -> {
                    HWIDProvider hwidProvider = new HWIDProvider();
                    return featureAPI.sendHardwareInfo(hwidProvider.getStatisticData(), null);
                }
                case ALL -> {
                    HWIDProvider hwidProvider = new HWIDProvider();
                    return featureAPI.sendHardwareInfo(hwidProvider.getStatisticData(), hwidProvider.getIdentifyData());
                }
            }
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        });
    }

    public Path initVfsDirectory() {
        Path defaultPath = Path.of("runtime");
        if(isTestMode()) {
            Vfs.get().put(defaultPath, new FileVfsDirectory(defaultPath));
        } else {
            var encryptKey = Launcher.getConfig().runtimeEncryptKey;
            if(encryptKey == null) {
                for(var e : Launcher.getConfig().runtime.entrySet()) {
                    var realPath = e.getKey();
                    var encodedName = "runtime/" + realPath;
                    try {
                        Vfs.get().put(defaultPath.resolve(realPath), new UrlVfsFile(IOHelper.getResourceURL(encodedName)));
                    } catch (NoSuchFileException ignored) {
                    }
                }
            } else {
                for(var e : Launcher.getConfig().runtime.entrySet()) {
                    var realPath = e.getKey();
                    var hash = e.getValue();
                    var encodedName = "runtime/" + SecurityHelper.toHex(hash);
                    try {
                        Vfs.get().put(defaultPath.resolve(realPath), new CachedVfsFile(new EncryptedVfsFile(new UrlVfsFile(IOHelper.getResourceURL(encodedName)))));
                    } catch (NoSuchFileException ignored) {
                    }
                }
            }
        }
        if(LogHelper.isDevEnabled()) {
            Vfs.get().debugPrint(LogHelper.Level.DEV);
        }
        return defaultPath;
    }
}
