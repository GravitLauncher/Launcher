package pro.gravit.launcher.runtime.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.Downloader;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.request.RequestOfflineModeAPIImpl;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class LauncherBackendImpl implements LauncherBackendAPI, TextureUploadExtension, AutoCloseable {

    private static final Logger logger =
            LoggerFactory.getLogger(LauncherBackendImpl.class);
    /** How long before its expiry the access token is refreshed for a launch, to outlast the client's start. */
    private static final long LAUNCH_TOKEN_MARGIN_SECONDS = 60;

    private final ClientDownloadImpl clientDownloadImpl = new ClientDownloadImpl(this);
    private final JavaRuntimeService javaRuntimeService = new JavaRuntimeService(logger);
    private final OfflineProfileService offlineProfileService = new OfflineProfileService(logger);
    private final ProfileService profileService = new ProfileService();
    private volatile MainCallback callback;
    ExecutorService executorService;
    private volatile AuthMethod authMethod;
    // Settings
    private SettingsManager settingsManager;
    private NewLauncherSettings allSettings;
    private BackendSettings backendSettings;
    private OfflineModeStorageData offlineModeStorageData;
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
    private record PingKey(UUID profileId, boolean aggregate) {}
    private final Map<PingKey, CompletableFuture<ServerPingInfo>> pingFutures = new ConcurrentHashMap<>();
    private final ExecutorService pingExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean relocatingUpdates = new AtomicBoolean();
    private final java.util.concurrent.atomic.AtomicInteger activeDownloads = new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger activeRuntimes = new java.util.concurrent.atomic.AtomicInteger();
    /** Invalidates asynchronous results when the user changes identity or exits. */
    private final AuthSessionService authSessionService = new AuthSessionService();

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
        offlineProfileService.initialize(DirBridge.dir.resolve("profiles.json"));
        offlineModeStorageData = offlineProfileService.storage();
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
                        logger.error("", e);
                    }
                }));
            }
            if(backendSettings.authId != null) {
                for(var e : authMethods) {
                    if(backendSettings.authId.equals(e.getName())) {
                        selectAuthMethod(e);
                        break;
                    }
                }
            }
            return new LauncherInitData(authMethods);
        }, executorService);
    }

    public AuthFeatureAPI.AuthToken getAuthToken() {
        return backendSettings.auth.toToken();
    }

    @Override
    public AuthMethod getAuthMethod() {
        return authMethod;
    }

    @Override
    public void selectAuthMethod(AuthMethod method) {
        Objects.requireNonNull(method, "method");
        synchronized (authSessionService.lock()) {
            authSessionService.advance();
            boolean wasAuthorized = selfUser != null;
            AuthMethod oldAuthMethod = this.authMethod;
            this.authMethod = method;
            if(this.backendSettings.auths.containsKey(method.getName())) {
                this.backendSettings.auth = this.backendSettings.auths.get(method.getName());
            } else if(oldAuthMethod != null) {
                this.backendSettings.auth = null;
            }
            this.selfUser = null;
            this.permissions = new ClientPermissions();
            this.profiles = null;
            clearPings();
            this.backendSettings.authId = method.getName();
            if (wasAuthorized) callback.onExit();
        }
        LauncherAPIHolder.changeAuthId(method.getName());
    }

    @Override
    public CompletableFuture<SelfUser> tryAuthorize() {
        BackendSettings.AuthorizationData auth;
        long generation;
        synchronized (authSessionService.lock()) {
            if(this.authMethod == null) {
                logger.error("Trying to call tryAuthorize() before select Auth method");
                return CompletableFuture.failedFuture(new LauncherBackendException("This method call not allowed before select authMethod"));
            }
            auth = backendSettings.auth;
            if(auth == null) {
                logger.error("Trying to call tryAuthorize() - auth data not found");
                return CompletableFuture.failedFuture(new LauncherBackendException("Auth data not found"));
            }
            generation = authSessionService.current();
        }
        if(auth.expireIn > 0 && LocalDateTime.ofEpochSecond(auth.expireIn, 0, ZoneOffset.UTC).isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            logger.debug("tryAuthorize() - need refresh token");
            return refreshAuth(generation).thenApply(user -> user);
        }
        return LauncherAPIHolder.auth().restore(auth.accessToken, true).thenApply((user) -> {
            onAuthorize(generation, user);
            return user;
        });
    }

    /** Shares a token refresh across concurrent authorize/launch requests. */
    private CompletableFuture<SelfUser> refreshAuth(long generation) {
        return authSessionService.singleFlight(generation, () -> {
            var auth = backendSettings.auth;
            if (auth == null || auth.refreshToken == null) {
                return CompletableFuture.failedFuture(new LauncherBackendException("Refresh token not found"));
            }
            return LauncherAPIHolder.auth().refreshToken(auth.refreshToken)
                    .thenCompose(response -> {
                        String accessToken;
                        synchronized (authSessionService.lock()) {
                            if (!authSessionService.isCurrent(generation)) return CompletableFuture.failedFuture(new LauncherBackendException("Authentication session changed"));
                            setAuthToken(response);
                            accessToken = backendSettings.auth.accessToken;
                        }
                        return LauncherAPIHolder.auth().restore(accessToken, true);
                    }).thenApply(user -> {
                        onAuthorize(generation, user);
                        return user;
                    });
        });
    }

    private void setAuthToken(AuthFeatureAPI.AuthToken authToken) {
        backendSettings.auth = new BackendSettings.AuthorizationData();
        backendSettings.auth.accessToken = authToken.getAccessToken();
        backendSettings.auth.refreshToken = authToken.getRefreshToken();
        if(authToken.getExpire() <= 0) {
            backendSettings.auth.expireIn = 0;
        } else {
            backendSettings.auth.expireIn = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(authToken.getExpire()).toEpochSecond(ZoneOffset.UTC);
        }
        if(authMethod != null) {
            backendSettings.auths.put(authMethod.getName(), backendSettings.auth);
        }
    }

    private void onAuthorize(SelfUser selfUser) {
        this.selfUser = selfUser;
        permissions = selfUser.getPermissions();
        if(LauncherAPIHolder.auth() instanceof RequestOfflineModeAPIImpl impl) offlineProfileService.configure(impl, offlineModeStorageData);
        callback.onAuthorize(selfUser);
        if(processHardwareFuture == null) {
            processHardwareFuture = processHardware();
        }
    }

    private void onAuthorize(long generation, SelfUser user) {
        synchronized (authSessionService.lock()) {
            if (!authSessionService.isCurrent(generation)) throw new LauncherBackendException("Authentication session changed");
            onAuthorize(user);
        }
    }

    @Override
    public CompletableFuture<SelfUser> authorize(String login, AuthMethodPassword password) {
        long generation;
        synchronized (authSessionService.lock()) {
            if(this.authMethod == null) {
                return CompletableFuture.failedFuture(new LauncherBackendException("This method call not allowed before select authMethod"));
            }
            generation = authSessionService.advance();
            clearCurrentSession();
            clearPings();
            callback.onExit();
        }
        return LauncherAPIHolder.auth().auth(login, password).thenApply((response) -> {
            synchronized (authSessionService.lock()) {
                if (!authSessionService.isCurrent(generation)) throw new LauncherBackendException("Authentication session changed");
                setAuthToken(response.authToken());
                onAuthorize(response.user());
            }
            return response.user();
        });
    }

    @Override
    public CompletableFuture<Void> userExit() {
        synchronized (authSessionService.lock()) {
            authSessionService.advance();
            clearCurrentSession();
            clearPings();
            if(authMethod != null) backendSettings.auths.remove(authMethod.getName());
            callback.onExit();
        }
        return LauncherAPIHolder.auth().exit().thenAccept(e -> { });
    }

    private void clearCurrentSession() {
        backendSettings.auth = null;
        this.selfUser = null;
        permissions = new ClientPermissions();
        profiles = null;
    }

    @Override
    public CompletableFuture<List<ProfileFeatureAPI.ClientProfile>> fetchProfiles() {
        long generation = authSessionService.current();
        return LauncherAPIHolder.profile().getProfiles().thenApply((profiles) -> {
            synchronized (authSessionService.lock()) {
                if (!authSessionService.isCurrent(generation)) throw new LauncherBackendException("Authentication session changed");
                onProfiles(profiles);
                callback.onProfiles(profiles);
            }
            return profiles;
        });
    }

    private void onProfiles(List<ProfileFeatureAPI.ClientProfile> profiles) {
        this.profiles = profileService.prepare(profiles);
        for(var e : backendSettings.settings.entrySet()) {
            ClientProfile profile = (ClientProfile) this.profiles.get(e.getKey());
            if(profile == null) {
                continue;
            }
            e.getValue().initAfterGson(profile, this);
        }
        if(offlineModeStorageData != null) {
            offlineProfileService.updateProfiles(profiles);
        }
    }

    @Override
    public ClientProfileSettings makeClientProfileSettings(ProfileFeatureAPI.ClientProfile profile) {
        var settings = backendSettings.settings.get(profile.getUUID());
        if(settings == null) {
            settings = new ProfileSettingsImpl((ClientProfile) profile, this);
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
        synchronized (relocatingUpdates) {
            if (relocatingUpdates.get()) return CompletableFuture.failedFuture(new IllegalStateException("Updates directory relocation is in progress"));
            activeDownloads.incrementAndGet();
        }
        long generation = authSessionService.current();
        return clientDownloadImpl.downloadProfile((ClientProfile) profile, (ProfileSettingsImpl) settings, callback)
                .thenCompose(readyProfile -> refreshTokenBeforeLaunch(generation).thenApply(v -> readyProfile))
                .thenApply(readyProfile -> {
                    if (!authSessionService.isCurrent(generation)) throw new LauncherBackendException("Authentication session changed");
                    return readyProfile;
                }).whenComplete((value, error) -> activeDownloads.decrementAndGet());
    }

    /**
     * {@link ReadyProfileImpl#run} hands the stored access token to the client, and the client restores its
     * session with it as is. {@link #tryAuthorize()} refreshes an expired token only when the launcher starts,
     * so a launcher left open longer than the token lives used to start the client with an expired token, and
     * the client stopped with auth.expiretoken. Refresh it here, once the files are in place, right before run.
     */
    private CompletableFuture<Void> refreshTokenBeforeLaunch(long generation) {
        BackendSettings.AuthorizationData auth;
        synchronized (authSessionService.lock()) {
            if (!authSessionService.isCurrent(generation)) {
                return CompletableFuture.failedFuture(new LauncherBackendException("Authentication session changed"));
            }
            auth = backendSettings.auth;
        }
        if(auth == null || auth.expireIn <= 0 || auth.refreshToken == null || LauncherAPIHolder.auth() instanceof RequestOfflineModeAPIImpl) {
            return CompletableFuture.completedFuture(null);
        }
        var refreshFrom = LocalDateTime.ofEpochSecond(auth.expireIn, 0, ZoneOffset.UTC).minusSeconds(LAUNCH_TOKEN_MARGIN_SECONDS);
        if(refreshFrom.isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            return CompletableFuture.completedFuture(null);
        }
        logger.debug("downloadProfile() - access token expired or about to, refresh it before launch");
        return refreshAuth(generation).thenAccept(user -> { });
    }

    @Override
    public CompletableFuture<byte[]> fetchTexture(Texture texture) {
        try {
            URI uri = URI.create(texture.getUrl());
            String scheme = uri.getScheme();
            if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) || uri.getHost() == null) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Texture URL must use HTTP or HTTPS"));
            }
            var request = java.net.http.HttpRequest.newBuilder(uri).timeout(java.time.Duration.ofSeconds(15)).GET().build();
            var client = Downloader.newHttpClientBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
            var requestFuture = client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());
            var responseBody = new java.util.concurrent.atomic.AtomicReference<InputStream>();
            var timedOutOrCancelled = new java.util.concurrent.atomic.AtomicBoolean();
            var result = requestFuture.thenApplyAsync(response -> {
                responseBody.set(response.body());
                if (timedOutOrCancelled.get()) {
                    try { response.body().close(); } catch (IOException ignored) { }
                    throw new java.util.concurrent.CompletionException(new java.util.concurrent.CancellationException());
                }
                try (var input = response.body()) {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("Texture request failed with HTTP " + response.statusCode());
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int total = 0;
                    for (int n; (n = input.read(buffer)) >= 0;) {
                        total += n;
                        if (total > 8 * 1024 * 1024) throw new IOException("Texture exceeds 8 MiB limit");
                        output.write(buffer, 0, n);
                    }
                    return output.toByteArray();
                } catch (IOException e) { throw new java.util.concurrent.CompletionException(e); }
            }).orTimeout(15, java.util.concurrent.TimeUnit.SECONDS);
            result.whenComplete((value, error) -> {
                if (error != null) {
                    timedOutOrCancelled.set(true);
                    requestFuture.cancel(true);
                    InputStream input = responseBody.get();
                    if (input != null) try { input.close(); } catch (IOException ignored) { }
                }
                client.shutdown();
            });
            return result;
        } catch (Throwable e) { return CompletableFuture.failedFuture(e); }
    }

    static void copyUpdatesDirectory(Path sourcePath, Path targetPath) throws IOException {
        Path source = sourcePath.toAbsolutePath().normalize();
        Path target = targetPath.toAbsolutePath().normalize();
        if (source.equals(target) || source.startsWith(target) || target.startsWith(source)) {
            throw new IllegalArgumentException("Updates directories cannot be equal or nested");
        }
        if (!Files.isDirectory(source)) throw new IOException("Updates directory does not exist: " + source);
        if (Files.exists(target)) {
            try (var entries = Files.list(target)) {
                if (entries.findAny().isPresent()) throw new IOException("Target updates directory is not empty: " + target);
            }
        }
        Path parent = target.getParent();
        if (parent == null) throw new IOException("Target updates directory has no parent");
        Files.createDirectories(parent);
        Path staging = Files.createTempDirectory(parent, target.getFileName() + ".relocating-");
        try {
            Files.walkFileTree(source, new java.nio.file.SimpleFileVisitor<>() {
                @Override public java.nio.file.FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                    Files.createDirectories(staging.resolve(source.relativize(dir)));
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
                @Override public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, staging.resolve(source.relativize(file)), java.nio.file.StandardCopyOption.COPY_ATTRIBUTES);
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
            });
            if (Files.exists(target)) Files.delete(target);
            try {
                Files.move(staging, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(staging, target);
            }
        } catch (IOException | RuntimeException | Error e) {
            try { pro.gravit.utils.helper.IOHelper.deleteDir(staging, true); }
            catch (IOException cleanupError) { e.addSuppressed(cleanupError); }
            throw e;
        }
    }

    void runtimeStarted() {
        synchronized (relocatingUpdates) {
            if (relocatingUpdates.get()) throw new IllegalStateException("Updates directory relocation is in progress");
            activeRuntimes.incrementAndGet();
        }
    }

    void runtimeStopped() { activeRuntimes.decrementAndGet(); }

    public CompletableFuture<Void> relocateUpdatesDirectory(Path path) {
        final Path source;
        final Path target;
        try {
            source = DirBridge.dirUpdates.toAbsolutePath().normalize();
            target = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
            if (source.equals(target) || source.startsWith(target) || target.startsWith(source)) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Updates directories cannot be equal or nested"));
            }
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
        synchronized (relocatingUpdates) {
            if (activeDownloads.get() != 0 || activeRuntimes.get() != 0 || !relocatingUpdates.compareAndSet(false, true)) {
                return CompletableFuture.failedFuture(new IllegalStateException("Updates directory is in use or relocation is already running"));
            }
        }
        return CompletableFuture.runAsync(() -> {
            try {
                copyUpdatesDirectory(source, target);
                DirBridge.dirUpdates = target;
                synchronized (this) {
                    availableJavas = null;
                    availableJavasFuture = null;
                }
                if (backendSettings != null && backendSettings.settings != null) {
                    try {
                        for (ProfileSettingsImpl settings : backendSettings.settings.values()) settings.remapJavaPath(source, target);
                        if (settingsManager != null) settingsManager.saveConfig();
                    } catch (Throwable e) {
                        logger.error("Updates directory moved to {}, but Java path settings could not be saved", target, e);
                    }
                }
                try {
                    pro.gravit.utils.helper.IOHelper.deleteDir(source, true);
                } catch (IOException e) {
                    logger.warn("Unable to remove old updates directory {} after relocation", source, e);
                }
            } catch (Throwable e) {
                throw new java.util.concurrent.CompletionException(e);
            } finally {
                relocatingUpdates.set(false);
            }
        });
    }

    @Override
    public CompletableFuture<List<Java>> getAvailableJava() {
        synchronized (this) {
            if (availableJavas != null) return CompletableFuture.completedFuture(availableJavas);
            if(availableJavasFuture == null) {
                availableJavasFuture = CompletableFuture.supplyAsync(() -> {
                    return javaRuntimeService.discover();
                }, executorService).thenApply(List::copyOf).thenApply(e -> {
                    availableJavas = e;
                    return e;
                }).whenComplete((value, error) -> {
                    if (error != null) synchronized (LauncherBackendImpl.this) { availableJavasFuture = null; }
                });
            }
            return availableJavasFuture;
        }
    }

    public List<Java> getCustomJava() {
        return javaRuntimeService.customRuntimes();
    }

    public boolean isArchAvailable(JVMHelper.ARCH arch) {
        return javaRuntimeService.isArchAvailable(arch);
    }

    @Override
    public CompletableFuture<ServerPingInfo> pingServer(ProfileFeatureAPI.ClientProfile profile) {
        PingKey key = new PingKey(profile.getUUID(), false);
        CompletableFuture<ServerPingInfo> future = pingFutures.computeIfAbsent(key, (k) -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    ServerPinger pinger = new ServerPinger((ClientProfile) profile);
                    return pinger.ping();
                } catch (Throwable e) {
                    throw new java.util.concurrent.CompletionException(e);
                }
            }, pingExecutor);
        });
        future.whenComplete((value, error) -> pingFutures.remove(key, future));
        return future;
    }

    @Override
    public CompletableFuture<ServerPingInfo> pingProfileServers(ProfileFeatureAPI.ClientProfile profile) {
        PingKey key = new PingKey(profile.getUUID(), true);
        CompletableFuture<ServerPingInfo> shared = pingFutures.computeIfAbsent(key, (k) -> {
            CompletableFuture<ServerPingInfo> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> {
                try {
                    ClientProfile clientProfile = (ClientProfile) profile;
                    List<ClientProfile.ServerProfile> servers = clientProfile.getServers();

                    if (servers == null || servers.isEmpty()) {
                        future.complete(new ServerPingInfo() {
                            @Override public int getMaxOnline() { return 0; }
                            @Override public int getOnline() { return 0; }
                            @Override public List<String> getPlayerNames() { return List.of(); }
                        });
                        return;
                    }

                    var serverPingFutures = servers.stream()
                            .map(server -> CompletableFuture.supplyAsync(() -> {
                                try {
                                    ServerPinger pinger = new ServerPinger(server, clientProfile.getVersion());
                                    return pinger.ping();
                                } catch (IOException e) {
                                    logger.warn("Failed to ping server {}: {}", server.name, e.getMessage());
                                    return null;
                                }
                            }, pingExecutor))
                            .toList();

                    CompletableFuture.allOf(serverPingFutures.toArray(new CompletableFuture[0]))
                            .thenAccept(v -> {
                                int totalOnline = 0;
                                int totalMax = 0;
                                boolean hasAnyResult = false;

                                for (var pingFuture : serverPingFutures) {
                                    try {
                                        ServerPinger.Result result = pingFuture.get();
                                        if (result != null) {
                                            totalOnline += result.onlinePlayers;
                                            totalMax += result.maxPlayers;
                                            hasAnyResult = true;
                                        }
                                    } catch (Exception e) {
                                        logger.warn("Failed to get ping result: {}", e.getMessage());
                                    }
                                }

                                if (!hasAnyResult) {
                                    future.completeExceptionally(new IOException("All servers are unavailable"));
                                    return;
                                }

                                int finalOnline = totalOnline;
                                int finalMax = totalMax;
                                future.complete(new ServerPingInfo() {
                                    @Override
                                    public int getMaxOnline() {
                                        return finalMax;
                                    }

                                    @Override
                                    public int getOnline() {
                                        return finalOnline;
                                    }

                                    @Override
                                    public List<String> getPlayerNames() {
                                        return List.of();
                                    }
                                });
                            })
                            .exceptionally(ex -> {
                                future.completeExceptionally(ex);
                                return null;
                            });
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            }, pingExecutor);
            return future;
        });
        shared.whenComplete((value, error) -> pingFutures.remove(key, shared));
        return shared;
    }

    private void clearPings() {
        for (var future : pingFutures.values()) future.cancel(true);
        pingFutures.clear();
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
        return selfUser == null ? "Player" : selfUser.getUsername();
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
        close();
    }

    private void shutdownResources() {
        clearPings();
        pingExecutor.shutdownNow();
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (settingsManager != null) {
            try {
                settingsManager.saveConfig();
            } catch (IOException e) {
                logger.error("Config not saved", e);
            }
            offlineProfileService.save();
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        synchronized (authSessionService.lock()) {
            authSessionService.advance();
            selfUser = null;
            permissions = new ClientPermissions();
            profiles = null;
            processHardwareFuture = null;
        }
        try {
            shutdownResources();
        } finally {
            LauncherAPIHolder.reset();
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
        long generation = authSessionService.current();
        HardwareVerificationFeatureAPI featureAPI = LauncherAPIHolder.get().get(HardwareVerificationFeatureAPI.class);
        if(featureAPI == null) {
            return CompletableFuture.completedFuture(null);
        }
        return featureAPI.getSecurityInfo().thenCompose((response) -> {
            if (!authSessionService.isCurrent(generation)) return CompletableFuture.failedFuture(new LauncherBackendException("Authentication session changed"));
            if(!response.isRequired()) {
                return CompletableFuture.completedFuture(null);
            }
            byte[] signature = SecurityHelper.sign(response.getSignData(), ecKeyHolder.privateKey);
            return featureAPI.privateKeyVerification(ecKeyHolder.publicKey, signature);
        }).thenCompose((response) -> {
            if (!authSessionService.isCurrent(generation)) return CompletableFuture.failedFuture(new LauncherBackendException("Authentication session changed"));
            if (response == null) return CompletableFuture.completedFuture(null);
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
        if(logger.isTraceEnabled()) {
            Vfs.get().debugPrint(Level.TRACE);
        }
        return defaultPath;
    }
}
