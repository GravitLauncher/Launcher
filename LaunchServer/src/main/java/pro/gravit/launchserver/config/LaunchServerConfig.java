package pro.gravit.launchserver.config;

import io.netty.channel.epoll.Epoll;
import io.netty.handler.logging.LogLevel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.core.RejectAuthCoreProvider;
import pro.gravit.launchserver.auth.profiles.LocalProfileProvider;
import pro.gravit.launchserver.auth.profiles.ProfileProvider;
import pro.gravit.launchserver.auth.protect.ProtectHandler;
import pro.gravit.launchserver.auth.protect.StdProtectHandler;
import pro.gravit.launchserver.auth.texture.RequestTextureProvider;
import pro.gravit.launchserver.auth.updates.LocalUpdatesProvider;
import pro.gravit.launchserver.auth.updates.UpdatesProvider;
import pro.gravit.launchserver.components.AuthLimiterComponent;
import pro.gravit.launchserver.components.Component;
import pro.gravit.launchserver.components.ProGuardComponent;

import java.util.*;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class LaunchServerConfig {
    private final static List<String> oldMirrorList = List.of("https://mirror.gravit.pro/5.2.x/", "https://mirror.gravit.pro/5.3.x/",
            "https://mirror.gravitlauncher.com/5.2.x/", "https://mirror.gravitlauncher.com/5.3.x/", "https://mirror.gravitlauncher.com/5.4.x/",
            "https://mirror.gravitlauncher.com/5.5.x/");
    private transient final Logger logger = LogManager.getLogger();
    public String projectName;
    public String[] mirrors;
    public String binaryName;
    public boolean copyBinaries = true;
    public LauncherConfig.LauncherEnvironment env;
    public Map<String, AuthProviderPair> auth;
    // Handlers & Providers
    public ProtectHandler protectHandler;
    public Map<String, Component> components;
    public ProfileProvider profileProvider = new LocalProfileProvider();
    public UpdatesProvider updatesProvider = new LocalUpdatesProvider();
    public NettyConfig netty;
    public LauncherConf launcher;
    public JarSignerConf sign;
    private transient LaunchServer server = null;
    private transient AuthProviderPair authDefault;

    public static LaunchServerConfig getDefault(LaunchServer.LaunchServerEnv env) {
        LaunchServerConfig newConfig = new LaunchServerConfig();
        newConfig.mirrors = new String[]{"https://mirror.gravitlauncher.com/5.6.x/", "https://gravit-launcher-mirror.storage.googleapis.com/"};
        newConfig.env = LauncherConfig.LauncherEnvironment.STD;
        newConfig.auth = new HashMap<>();
        AuthProviderPair a = new AuthProviderPair(new RejectAuthCoreProvider(),
                new RequestTextureProvider("http://example.com/skins/%username%.png", "http://example.com/cloaks/%username%.png")
        );
        a.displayName = "Default";
        newConfig.auth.put("std", a);
        newConfig.protectHandler = new StdProtectHandler();
        newConfig.binaryName = "Launcher";

        newConfig.netty = new NettyConfig();
        newConfig.netty.fileServerEnabled = true;
        newConfig.netty.binds = new NettyBindAddress[]{new NettyBindAddress("0.0.0.0", 9274)};
        newConfig.netty.performance = new NettyPerformanceConfig();
        try {
            newConfig.netty.performance.usingEpoll = Epoll.isAvailable();
        } catch (Throwable e) {
            // Epoll class line 51+ catch (Exception) but Error will be thrown by System.load
            newConfig.netty.performance.usingEpoll = false;
        } // such as on ARM
        newConfig.netty.performance.bossThread = 2;
        newConfig.netty.performance.workerThread = 8;
        newConfig.netty.performance.schedulerThread = 2;

        newConfig.launcher = new LauncherConf();
        newConfig.launcher.compress = true;
        newConfig.launcher.deleteTempFiles = true;
        newConfig.launcher.stripLineNumbers = true;
        newConfig.launcher.customJvmOptions.add("-Dfile.encoding=UTF-8");

        newConfig.sign = new JarSignerConf();

        newConfig.components = new HashMap<>();
        AuthLimiterComponent authLimiterComponent = new AuthLimiterComponent();
        authLimiterComponent.rateLimit = 3;
        authLimiterComponent.rateLimitMillis = SECONDS.toMillis(8);
        authLimiterComponent.message = "Превышен лимит авторизаций";
        newConfig.components.put("authLimiter", authLimiterComponent);
        ProGuardComponent proGuardComponent = new ProGuardComponent();
        newConfig.components.put("proguard", proGuardComponent);
        newConfig.profileProvider = new LocalProfileProvider();
        return newConfig;
    }

    public void setLaunchServer(LaunchServer server) {
        this.server = server;
    }

    public AuthProviderPair getAuthProviderPair(String name) {
        return auth.get(name);
    }

    public AuthProviderPair getAuthProviderPair() {
        if (authDefault != null) return authDefault;
        for (AuthProviderPair pair : auth.values()) {
            if (pair.isDefault) {
                authDefault = pair;
                return pair;
            }
        }
        throw new IllegalStateException("Default AuthProviderPair not found");
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setBinaryName(String binaryName) {
        this.binaryName = binaryName;
    }

    public void setEnv(LauncherConfig.LauncherEnvironment env) {
        this.env = env;
    }

    public void verify() {
        if (auth == null || auth.isEmpty()) {
            throw new NullPointerException("AuthProviderPair`s count should be at least one");
        }

        boolean isOneDefault = false;
        for (AuthProviderPair pair : auth.values()) {
            if (pair.isDefault) {
                isOneDefault = true;
                break;
            }
        }
        if (protectHandler == null) {
            throw new NullPointerException("ProtectHandler must not be null");
        }
        if (!isOneDefault) {
            throw new IllegalStateException("No auth pairs declared by default.");
        }
        if (env == null) {
            throw new NullPointerException("Env must not be null");
        }
        if (netty == null) {
            throw new NullPointerException("Netty must not be null");
        }
        // Mirror check
        {
            boolean updateMirror = Boolean.getBoolean("launchserver.config.disableUpdateMirror");
            if (!updateMirror) {
                for (int i = 0; i < mirrors.length; ++i) {
                    if (mirrors[i] != null && oldMirrorList.contains(mirrors[i])) {
                        logger.warn("Replace mirror '{}' to 'https://mirror.gravitlauncher.com/5.6.x/'. If you really need to use original url, use '-Dlaunchserver.config.disableUpdateMirror=true'", mirrors[i]);
                        mirrors[i] = "https://mirror.gravitlauncher.com/5.6.x/";
                    }
                }
            }
        }
    }

    public void init(LaunchServer.ReloadType type) {
        Launcher.applyLauncherEnv(env);
        for (Map.Entry<String, AuthProviderPair> provider : auth.entrySet()) {
            provider.getValue().init(server, provider.getKey());
        }
        if (protectHandler != null) {
            server.registerObject("protectHandler", protectHandler);
            protectHandler.init(server);
        }
        if(profileProvider != null) {
            server.registerObject("profileProvider", profileProvider);
            profileProvider.init(server);
        }
        if(updatesProvider != null) {
            server.registerObject("updatesProvider", updatesProvider);
            updatesProvider.init(server);
        }
        if (components != null) {
            components.forEach((k, v) -> server.registerObject("component.".concat(k), v));
        }
        if (!type.equals(LaunchServer.ReloadType.NO_AUTH)) {
            for (AuthProviderPair pair : auth.values()) {
                server.registerObject("auth.".concat(pair.name).concat(".core"), pair.core);
                server.registerObject("auth.".concat(pair.name).concat(".texture"), pair.textureProvider);
            }
        }
        Arrays.stream(mirrors).forEach(server.mirrorManager::addMirror);
    }

    public void close(LaunchServer.ReloadType type) {
        try {
            if (!type.equals(LaunchServer.ReloadType.NO_AUTH)) {
                for (AuthProviderPair pair : auth.values()) {
                    server.unregisterObject("auth.".concat(pair.name).concat(".core"), pair.core);
                    server.unregisterObject("auth.".concat(pair.name).concat(".texture"), pair.textureProvider);
                    pair.close();
                }
            }
            if (type.equals(LaunchServer.ReloadType.FULL)) {
                components.forEach((k, component) -> {
                    server.unregisterObject("component.".concat(k), component);
                    if (component instanceof AutoCloseable autoCloseable) {
                        try {
                            autoCloseable.close();
                        } catch (Exception e) {
                            logger.error(e);
                        }
                    }
                });
            }
        } catch (Exception e) {
            logger.error(e);
        }
        if (protectHandler != null) {
            server.unregisterObject("protectHandler", protectHandler);
            protectHandler.close();
        }
        if(profileProvider != null) {
            server.unregisterObject("profileProvider", profileProvider);
            profileProvider.close();
        }
        if(updatesProvider != null) {
            server.unregisterObject("updatesProvider", updatesProvider);
            updatesProvider.close();
        }
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

    public static class NettyUpdatesBind {
        public String url;
        public boolean zip;
    }

    public static class LauncherConf {
        public boolean compress;
        public boolean stripLineNumbers;
        public boolean deleteTempFiles;
        public boolean certificatePinning;
        public boolean encryptRuntime;
        public List<String> customJvmOptions = new ArrayList<>();
        public int memoryLimit = 256;
    }

    public static class NettyConfig {
        public boolean fileServerEnabled;
        public boolean ipForwarding;
        public boolean disableWebApiInterface;
        public boolean showHiddenFiles;
        public boolean sendProfileUpdatesEvent = true;
        public String launcherURL;
        public String downloadURL;
        public String launcherEXEURL;
        public String address;
        public Map<String, LaunchServerConfig.NettyUpdatesBind> bindings = new HashMap<>();
        public NettyPerformanceConfig performance;

        public NettySecurityConfig security = new NettySecurityConfig();
        public NettyBindAddress[] binds;
        public LogLevel logLevel = LogLevel.DEBUG;
    }

    public static class NettyPerformanceConfig {
        public boolean usingEpoll;
        public int bossThread;
        public int workerThread;
        public int schedulerThread;
        public int maxWebSocketRequestBytes = 1024 * 1024;
        public boolean disableThreadSafeClientObject;
        public NettyExecutorType executorType = NettyExecutorType.VIRTUAL_THREADS;

        public enum NettyExecutorType {
            NONE, DEFAULT, WORK_STEAL, VIRTUAL_THREADS
        }
    }

    public static class NettyBindAddress {
        public String address;
        public int port;

        public NettyBindAddress(String address, int port) {
            this.address = address;
            this.port = port;
        }
    }

    public static class NettySecurityConfig {
        public long hardwareTokenExpire = HOURS.toSeconds(8);
        public long publicKeyTokenExpire = HOURS.toSeconds(8);

        public long launcherTokenExpire = HOURS.toSeconds(8);
    }
}
