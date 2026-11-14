package pro.gravit.launchserver.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.base.ApiMode;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.profiles.LocalProfilesProvider;
import pro.gravit.launchserver.profiles.ProfilesProvider;
import pro.gravit.launchserver.distribution.LocalUpdatesProvider;
import pro.gravit.launchserver.distribution.UpdatesProvider;
import pro.gravit.launchserver.components.Component;
import pro.gravit.launchserver.components.ProGuardComponent;

import java.io.Reader;
import java.net.URI;
import java.util.*;

public final class LaunchServerConfig {
    public static final String PROFILES_PROVIDER_OBJECT_ID = "profilesProvider";
    public static final String UPDATES_PROVIDER_OBJECT_ID = "updatesProvider";
    private static final String COMPONENT_OBJECT_PREFIX = "component.";
    public String projectName;
    public String[] mirrors;
    public LauncherConfig.LauncherEnvironment env;
    // Handlers & Providers
    public Map<String, Component> components;
    public ProfilesProvider profilesProvider = new LocalProfilesProvider();
    public UpdatesProvider updatesProvider = new LocalUpdatesProvider();
    public String apiUrl;
    public String distributionUrl;
    public SchedulerConfig scheduler;
    public LauncherConf launcher;
    public JarSignerConf sign;
    public boolean experimentalDevOnlyFeatures;
    private transient LaunchServer server = null;
    private transient boolean profileProviderRegistered;
    private transient boolean updatesProviderRegistered;
    private transient boolean profileProviderInitAttempted;
    private transient boolean updatesProviderInitAttempted;
    private transient final Set<String> registeredComponents = new LinkedHashSet<>();
    private transient final Set<String> componentInitAttempts = new LinkedHashSet<>();

    public static LaunchServerConfig getDefault(LaunchServer.LaunchServerEnv env) {
        LaunchServerConfig newConfig = new LaunchServerConfig();
        newConfig.mirrors = new String[]{"https://mirror.gravitlauncher.com/5.7.x/"};
        newConfig.env = LauncherConfig.LauncherEnvironment.STD;

        newConfig.apiUrl = "http://localhost:9274/api";
        newConfig.distributionUrl = "http://localhost:9274/%dirname%/";
        newConfig.scheduler = new SchedulerConfig();

        newConfig.launcher = new LauncherConf();
        newConfig.launcher.compress = true;
        newConfig.launcher.deleteTempFiles = true;
        newConfig.launcher.stripLineNumbers = true;
        newConfig.launcher.customJvmOptions.add("-Dfile.encoding=UTF-8");

        newConfig.sign = new JarSignerConf();

        newConfig.components = new HashMap<>();
        ProGuardComponent proGuardComponent = new ProGuardComponent();
        newConfig.components.put("proguard", proGuardComponent);
        newConfig.profilesProvider = new LocalProfilesProvider();
        newConfig.updatesProvider = new LocalUpdatesProvider();
        return newConfig;
    }

    public static LaunchServerConfig readConfig(Reader reader) {
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        if (json.has("netty")) {
            throw new IllegalStateException("This configuration uses the removed Netty server. Migrate netty.address to apiUrl and netty.downloadURL to distributionUrl, and configure the HTTP server separately.");
        }
        JsonObject launcher = objectAt(json, "launcher");
        if (launcher != null && launcher.has("experimentalDevOnlyHttpApi")) {
            throw new IllegalStateException("launcher.experimentalDevOnlyHttpApi has been removed; select launcher.api.mode instead.");
        }
        JsonObject api = objectAt(launcher, "api");
        if (api != null && api.has("mode") && "WEBSOCKET".equalsIgnoreCase(api.get("mode").getAsString())) {
            throw new IllegalStateException("launcher.api.mode=WEBSOCKET has been removed; use HTTP.");
        }
        JsonObject microsoft = objectAt(api, "microsoft");
        if (microsoft != null && microsoft.has("clientSecret")) {
            throw new IllegalStateException("launcher.api.microsoft.clientSecret is no longer supported; Microsoft authentication uses the public client flow.");
        }
        return Launcher.gsonManager.gson.fromJson(json, LaunchServerConfig.class);
    }

    private static JsonObject objectAt(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonObject() ? object.getAsJsonObject(key) : null;
    }

    public void setLaunchServer(LaunchServer server) {
        this.server = server;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setEnv(LauncherConfig.LauncherEnvironment env) {
        this.env = env;
    }

    public void verify() {
        if (env == null) {
            throw new NullPointerException("Env must not be null");
        }
        verifyHttpUrl(apiUrl, "API URL");
        verifyHttpUrl(distributionUrl, "Distribution URL");
        if (scheduler == null || scheduler.threads < 1) {
            throw new IllegalStateException("Scheduler threads must be positive");
        }
        if (mirrors == null || Arrays.stream(mirrors).anyMatch(Objects::isNull)) {
            throw new IllegalStateException("Mirrors must not be null");
        }
        if (components == null || components.entrySet().stream().anyMatch(e -> e.getKey() == null || e.getValue() == null)) {
            throw new IllegalStateException("Components and component names must not be null");
        }
        if (launcher == null || launcher.api == null || launcher.api.mode == null || launcher.api.microsoft == null || sign == null) {
            throw new IllegalStateException("Launcher API, Microsoft API, and signing configuration must not be null");
        }
    }

    private static void verifyHttpUrl(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalStateException(label + " must not be blank");
        try {
            URI uri = URI.create(value.replace("%dirname%", "dirname"));
            if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(label + " must be an absolute HTTP(S) URL", e);
        }
    }

    public void inheritComponents(LaunchServerConfig previous) {
        if (previous == this) throw new IllegalArgumentException("Cannot inherit components from the same config");
        components = previous.components;
        registeredComponents.addAll(previous.registeredComponents);
        previous.registeredComponents.clear();
        componentInitAttempts.addAll(previous.componentInitAttempts);
        previous.componentInitAttempts.clear();
    }

    public void init(LaunchServer.ReloadType type) {
        if(profilesProvider != null) {
            server.registerObject(PROFILES_PROVIDER_OBJECT_ID, profilesProvider);
            profileProviderRegistered = true;
            profileProviderInitAttempted = true;
            profilesProvider.init(server);
        }
        if(updatesProvider != null) {
            server.registerObject(UPDATES_PROVIDER_OBJECT_ID, updatesProvider);
            updatesProviderRegistered = true;
            updatesProviderInitAttempted = true;
            updatesProvider.init(server);
        }
        if (type == LaunchServer.ReloadType.FULL && components != null) {
            components.forEach((k, v) -> {
                String objectId = COMPONENT_OBJECT_PREFIX.concat(k);
                server.registerObject(objectId, v);
                registeredComponents.add(objectId);
            });
        }
        Arrays.stream(mirrors).forEach(server.mirrorManager::addMirror);
    }

    public void initComponents() {
        if (components == null) return;
        components.forEach((name, component) -> {
            componentInitAttempts.add(name);
            component.setComponentName(name);
            component.init(server);
        });
    }

    public void close(LaunchServer.ReloadType type) {
        Throwable failure = null;
        if (updatesProvider != null && updatesProviderRegistered) {
            updatesProviderRegistered = false;
            try {
                server.unregisterObject(UPDATES_PROVIDER_OBJECT_ID, updatesProvider);
            } catch (Throwable e) {
                failure = e;
            }
        }
        if (updatesProvider != null && updatesProviderInitAttempted) {
            updatesProviderInitAttempted = false;
            try {
                updatesProvider.close();
            } catch (Throwable e) {
                failure = appendFailure(failure, e);
            }
        }
        if (profilesProvider != null && profileProviderRegistered) {
            profileProviderRegistered = false;
            try {
                server.unregisterObject(PROFILES_PROVIDER_OBJECT_ID, profilesProvider);
            } catch (Throwable e) {
                failure = appendFailure(failure, e);
            }
        }
        if (profilesProvider != null && profileProviderInitAttempted) {
            profileProviderInitAttempted = false;
            try {
                profilesProvider.close();
            } catch (Throwable e) {
                failure = appendFailure(failure, e);
            }
        }
        if (type == LaunchServer.ReloadType.FULL && components != null) {
            for (var entry : components.entrySet()) {
                Component component = entry.getValue();
                boolean initAttempted = componentInitAttempts.remove(entry.getKey());
                String objectId = COMPONENT_OBJECT_PREFIX.concat(entry.getKey());
                try {
                    if (registeredComponents.remove(objectId)) server.unregisterObject(objectId, component);
                } catch (Throwable e) {
                    failure = appendFailure(failure, e);
                }
                if (!initAttempted) continue;
                if (component instanceof AutoCloseable autoCloseable) {
                    try {
                        autoCloseable.close();
                    } catch (Throwable e) {
                        failure = appendFailure(failure, e);
                    }
                }
            }
        }
        if (failure != null) {
            if (failure instanceof RuntimeException runtimeException) throw runtimeException;
            if (failure instanceof Error error) throw error;
            throw new IllegalStateException("Failed to close launch server configuration resources", failure);
        }
    }

    private static Throwable appendFailure(Throwable failure, Throwable next) {
        if (failure == null) return next;
        failure.addSuppressed(next);
        return failure;
    }

    public static class JarSignerConf {
        public boolean enabled = false;
        public String keyStore = "pathToKey";
        public String keyStoreType = "JKS";
        public String keyStorePass = "mypass";
        public String keyAlias = "myname";
        public String keyPass = "mypass";
        public String metaInfKeyName = "SIGNUMO.RSA";
        public String metaInfSfName = "SIGNUMO.SF";
        public String signAlgo = "SHA256WITHRSA";
        public boolean checkCertificateExpired = true;
    }

    public static class LauncherConf {
        public boolean compress;
        public boolean stripLineNumbers;
        public boolean deleteTempFiles;
        public boolean certificatePinning;
        public boolean encryptRuntime;
        public List<String> customJvmOptions = new ArrayList<>();
        public Map<String, String> customJavaDownload = new HashMap<>();
        public boolean forceUseCustomJava;
        public int memoryLimit = 256;
        public boolean alwaysUseRelauncher;
        public LauncherApiConf api = new LauncherApiConf();
    }

    public static class LauncherApiConf {
        public ApiMode mode = ApiMode.AUTO;
        public boolean offlineOnConnectionFail = true;
        public MicrosoftConf microsoft = new MicrosoftConf();
    }

    public static class MicrosoftConf {
        public boolean enabled;
        public String clientId = "d772766b-19b4-4f69-b353-989f890c5d3b";
    }

    public static class SchedulerConfig {
        public int threads = 2;
    }
}
