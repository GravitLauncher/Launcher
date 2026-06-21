package pro.gravit.launcher.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.base.modules.LauncherModulesManager;
import pro.gravit.launcher.base.request.*;
import pro.gravit.launcher.base.request.websockets.StdWebSocketService;
import pro.gravit.launcher.core.api.LauncherAPI;
import pro.gravit.launcher.core.api.LauncherAPIHolder;
import pro.gravit.launcher.core.api.features.*;
import pro.gravit.launcher.core.api.model.SelfUser;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class LauncherAPIInitializer {
    private static final Logger logger = LoggerFactory.getLogger(LauncherAPIInitializer.class);

    public static boolean isHttpAddress(String address) {
        return address.startsWith("http://") || address.startsWith("https://");
    }

    public enum Flag {
        OFFLINE_MODE,
        ENABLE_AUTO_REFRESH
    }

    public enum ApiMode {
        AUTO,
        WEBSOCKET,
        HTTP,
        MICROSOFT,
        OFFLINE
    }

    public static void initialize(LauncherModulesManager modulesManager, LauncherConfig config, List<Flag> flags) throws Exception {
        ApiMode mode = parseMode(config.launcherApiMode);
        if (config.useHttpApi && mode == ApiMode.AUTO) {
            mode = ApiMode.HTTP;
        }
        initialize(modulesManager, config.address, flags, new Options(mode, config.launcherApiOfflineOnConnectionFail,
                config.launcherApiMicrosoftEnabled || mode == ApiMode.MICROSOFT,
                config.launcherApiMicrosoftClientId, config.launcherApiMicrosoftClientSecret));
    }

    public static void initialize(LauncherModulesManager modulesManager, String address, List<Flag> flags) throws Exception {
        initialize(modulesManager, address, flags, new Options(ApiMode.AUTO, true, false, null, null));
    }

    public static void initialize(LauncherModulesManager modulesManager, String address, List<Flag> flags, Options options) throws Exception {
        ApiMode mode = resolveMode(address, flags, options);
        if(flags.contains(Flag.OFFLINE_MODE)) {
            initOfflineMode();
        } else if(mode == ApiMode.OFFLINE) {
            initOfflineMode();
        } else if(mode == ApiMode.MICROSOFT) {
            initMicrosoftMode(address, flags, options);
        } else if(mode == ApiMode.HTTP) {
            initHttpMode(address, options);
        } else if(mode == ApiMode.WEBSOCKET) {
            initWebSocketMode(address, flags, options);
        } else if(isHttpAddress(address)) {
            initHttpMode(address, options);
        } else {
            initWebSocketMode(address, flags, options);
        }
    }

    private static ApiMode parseMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return ApiMode.AUTO;
        }
        try {
            return ApiMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            logger.warn("Unknown launcher API mode '{}', fallback to AUTO", mode);
            return ApiMode.AUTO;
        }
    }

    private static ApiMode resolveMode(String address, List<Flag> flags, Options options) {
        if(flags.contains(Flag.OFFLINE_MODE)) {
            return ApiMode.OFFLINE;
        }
        if(options.mode() != ApiMode.AUTO) {
            return options.mode();
        }
        return isHttpAddress(address) ? ApiMode.HTTP : ApiMode.WEBSOCKET;
    }

    private static void initWebSocketMode(String address, List<Flag> flags, Options options) throws Exception {
        StdWebSocketService service;
        try {
            service = StdWebSocketService.initWebSockets(address).get();
        } catch (Throwable e) {
            logger.error("Connection failed", e);
            if (options.offlineOnConnectionFail()) {
                initOfflineMode();
            } else {
                throw new RequestException("Connection failed", e);
            }
            return;
        }
        Request.setRequestService(service);
        logger.debug("Restore sessions");
        Request.restore(false, false, true);
        service.registerEventHandler(new BasicLauncherEventHandler());
        service.reconnectCallback = () ->
        {
            logger.debug("WebSocket connect closed. Try reconnect");
            try {
                Request.reconnect();
            } catch (Exception e) {
                logger.error("", e);
                throw new RequestException("Connection failed", e);
            }
        };
        if(flags.contains(Flag.ENABLE_AUTO_REFRESH)) {
            Request.startAutoRefresh();
        }
        RequestCoreFeatureAPIImpl coreImpl = new RequestCoreFeatureAPIImpl(Request.getRequestService());
        setupApi(coreImpl, (authId) -> {
            var impl = new RequestFeatureAPIImpl(Request.getRequestService(), authId);
            return createApi(impl, impl, impl, impl, impl);
        }, options);
    }

    private static void initHttpMode(String address, Options options) {
        RequestFeatureHttpAPIImpl impl = new RequestFeatureHttpAPIImpl(address);
        setupApi(impl, (authId) -> createApi(impl, impl, impl, impl, impl), options);
    }

    private static void initMicrosoftMode(String address, List<Flag> flags, Options options) throws Exception {
        Options microsoftOptions = new Options(ApiMode.AUTO, options.offlineOnConnectionFail(), true,
                options.microsoftClientId(), options.microsoftClientSecret());
        if (isHttpAddress(address)) {
            initHttpMode(address, microsoftOptions);
        } else {
            initWebSocketMode(address, flags, microsoftOptions);
        }
    }

    private static void initOfflineMode() {
        RequestOfflineModeAPIImpl impl = new RequestOfflineModeAPIImpl();
        LauncherAPIHolder.setCoreAPI(impl);
        LauncherAPIHolder.setCreateApiFactory((authId) -> createApi(impl, impl, impl, null, impl));
    }

    private static void setupApi(CoreFeatureAPI core, Function<String, LauncherAPI> baseFactory, Options options) {
        if (!options.microsoftEnabled()) {
            LauncherAPIHolder.setCoreAPI(core);
            LauncherAPIHolder.setCreateApiFactory(baseFactory);
            return;
        }
        RequestMicrosoftAPIImpl microsoft = createMicrosoftApi(options);
        LauncherAPIHolder.setCoreAPI(new UnionCoreFeatureAPI(core, microsoft));
        LauncherAPIHolder.setCreateApiFactory((authId) -> isMicrosoftAuthId(authId) ? mergeAuth(baseFactory.apply(authId), microsoft) : baseFactory.apply(authId));
    }

    private static RequestMicrosoftAPIImpl createMicrosoftApi(Options options) {
        return new RequestMicrosoftAPIImpl(options.microsoftClientId() == null ? "d772766b-19b4-4f69-b353-989f890c5d3b" : options.microsoftClientId(),
                options.microsoftClientSecret());
    }

    private static LauncherAPI mergeAuth(LauncherAPI base, RequestMicrosoftAPIImpl microsoft) {
        return createApi(new MicrosoftAuthBridgeAPI(base.auth(), microsoft), base.user(), base.profile(),
                base.get(TextureUploadFeatureAPI.class), base.get(HardwareVerificationFeatureAPI.class));
    }

    private static boolean isMicrosoftAuthId(String authId) {
        return "microsoft".equals(authId);
    }

    private static LauncherAPI createApi(AuthFeatureAPI auth, UserFeatureAPI user, ProfileFeatureAPI profile,
                                         TextureUploadFeatureAPI textureUpload, HardwareVerificationFeatureAPI hardwareVerification) {
        Map<Class<? extends FeatureAPI>, FeatureAPI> map = new HashMap<>();
        if (auth != null) {
            map.put(AuthFeatureAPI.class, auth);
        }
        if (user != null) {
            map.put(UserFeatureAPI.class, user);
        }
        if (profile != null) {
            map.put(ProfileFeatureAPI.class, profile);
        }
        if (textureUpload != null) {
            map.put(TextureUploadFeatureAPI.class, textureUpload);
        }
        if (hardwareVerification != null) {
            map.put(HardwareVerificationFeatureAPI.class, hardwareVerification);
        }
        return new LauncherAPI(map);
    }

    public record Options(ApiMode mode, boolean offlineOnConnectionFail, boolean microsoftEnabled,
                          String microsoftClientId, String microsoftClientSecret) {
    }

    private record UnionCoreFeatureAPI(CoreFeatureAPI base, CoreFeatureAPI microsoft) implements CoreFeatureAPI {
        @Override
        public CompletableFuture<List<pro.gravit.launcher.core.api.method.AuthMethod>> getAuthMethods() {
            return base.getAuthMethods().thenCombine(microsoft.getAuthMethods(), (baseMethods, microsoftMethods) -> {
                List<pro.gravit.launcher.core.api.method.AuthMethod> result = new ArrayList<>(baseMethods.size() + microsoftMethods.size());
                result.addAll(baseMethods);
                result.addAll(microsoftMethods);
                return result;
            });
        }

        @Override
        public CompletableFuture<LauncherUpdateInfo> checkUpdates() {
            return base.checkUpdates();
        }
    }

    private record MicrosoftAuthBridgeAPI(AuthFeatureAPI base, RequestMicrosoftAPIImpl microsoft) implements AuthFeatureAPI {
        @Override
        public CompletableFuture<SelfUser> getCurrentUser() {
            return base.getCurrentUser();
        }

        @Override
        public CompletableFuture<AuthResponse> auth(String login, pro.gravit.launcher.core.api.method.AuthMethodPassword password) {
            return microsoft.auth(login, password).thenCompose(response ->
                    base.restoreFromExternal(response, true)
                            .thenApply(user -> new AuthResponse(user == null ? response.user() : user, response.authToken())));
        }

        @Override
        public CompletableFuture<AuthToken> refreshToken(String refreshToken) {
            return microsoft.refreshToken(refreshToken).thenCompose(token ->
                    base.restoreFromExternal(new AuthResponse(null, token), false).thenApply(user -> token));
        }

        @Override
        public CompletableFuture<SelfUser> restore(String accessToken, boolean fetchUser) {
            return microsoft.restore(accessToken, fetchUser).thenCompose(token ->
                    base.restoreFromExternal(new AuthResponse(null, new MicrosiftBridgedRestoreAuthToken(accessToken)), false).thenApply(user -> token));
        }

        @Override
        public CompletableFuture<Void> exit() {
            return base.exit().thenCompose(result -> microsoft.exit());
        }

        public record MicrosiftBridgedRestoreAuthToken(String token) implements AuthToken {

            @Override
            public String getAccessToken() {
                return token;
            }

            @Override
            public String getRefreshToken() {
                return null;
            }

            @Override
            public long getExpire() {
                return 0;
            }
        }
    }
}
