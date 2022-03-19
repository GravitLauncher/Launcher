package pro.gravit.launchserver.config;

import io.netty.channel.epoll.Epoll;
import io.netty.handler.logging.LogLevel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.core.RejectAuthCoreProvider;
import pro.gravit.launchserver.auth.protect.ProtectHandler;
import pro.gravit.launchserver.auth.protect.StdProtectHandler;
import pro.gravit.launchserver.auth.texture.RequestTextureProvider;
import pro.gravit.launchserver.binary.tasks.exe.Launch4JTask;
import pro.gravit.launchserver.components.AuthLimiterComponent;
import pro.gravit.launchserver.components.Component;
import pro.gravit.launchserver.components.ProGuardComponent;
import pro.gravit.launchserver.components.RegLimiterComponent;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.JVMHelper;

import java.io.File;
import java.util.*;

public final class LaunchServerConfig {
    private transient final Logger logger = LogManager.getLogger();
    public String projectName;
    public String[] mirrors;
    public String binaryName;
    public boolean copyBinaries = true;
    public boolean cacheUpdates = true;
    public LauncherConfig.LauncherEnvironment env;
    public Map<String, AuthProviderPair> auth;
    // Handlers & Providers
    public ProtectHandler protectHandler;
    public Map<String, Component> components;
    public ExeConf launch4j;
    public NettyConfig netty;
    public LauncherConf launcher;
    public JarSignerConf sign;
    public String startScript;
    private transient LaunchServer server = null;
    private transient AuthProviderPair authDefault;

    public static LaunchServerConfig getDefault(LaunchServer.LaunchServerEnv env) {
        LaunchServerConfig newConfig = new LaunchServerConfig();
        newConfig.mirrors = new String[]{"https://mirror.gravit.pro/5.2.x/", "https://gravit-launcher-mirror.storage.googleapis.com/"};
        newConfig.launch4j = new LaunchServerConfig.ExeConf();
        newConfig.launch4j.enabled = true;
        newConfig.launch4j.copyright = "© GravitLauncher Team";
        newConfig.launch4j.fileDesc = "GravitLauncher ".concat(Version.getVersion().getVersionString());
        newConfig.launch4j.fileVer = Version.getVersion().getVersionString().concat(".").concat(String.valueOf(Version.getVersion().patch));
        newConfig.launch4j.internalName = "Launcher";
        newConfig.launch4j.trademarks = "This product is licensed under GPLv3";
        newConfig.launch4j.txtFileVersion = "%s, build %d";
        newConfig.launch4j.txtProductVersion = "%s, build %d";
        newConfig.launch4j.productName = "GravitLauncher";
        newConfig.launch4j.productVer = newConfig.launch4j.fileVer;
        newConfig.launch4j.maxVersion = "1.8.999";
        newConfig.env = LauncherConfig.LauncherEnvironment.STD;
        newConfig.startScript = JVMHelper.OS_TYPE.equals(JVMHelper.OS.MUSTDIE) ? "." + File.separator + "start.bat" : "." + File.separator + "start.sh";
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
        newConfig.launcher.guardType = "no";
        newConfig.launcher.compress = true;
        newConfig.launcher.deleteTempFiles = true;
        newConfig.launcher.stripLineNumbers = true;

        newConfig.sign = new JarSignerConf();

        newConfig.components = new HashMap<>();
        AuthLimiterComponent authLimiterComponent = new AuthLimiterComponent();
        authLimiterComponent.rateLimit = 3;
        authLimiterComponent.rateLimitMillis = 8000;
        authLimiterComponent.message = "Превышен лимит авторизаций";
        newConfig.components.put("authLimiter", authLimiterComponent);
        RegLimiterComponent regLimiterComponent = new RegLimiterComponent();
        regLimiterComponent.rateLimit = 3;
        regLimiterComponent.rateLimitMillis = 1000 * 60 * 60 * 10; //Блок на 10 часов
        regLimiterComponent.message = "Превышен лимит регистраций";
        newConfig.components.put("regLimiter", regLimiterComponent);
        ProGuardComponent proGuardComponent = new ProGuardComponent();
        newConfig.components.put("proguard", proGuardComponent);
        return newConfig;
    }

    public LaunchServerConfig setLaunchServer(LaunchServer server) {
        this.server = server;
        return this;
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
        if (auth == null || auth.size() < 1) {
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
            if(!updateMirror) {
                for(int i=0;i < mirrors.length;++i) {
                    if("https://mirror.gravit.pro/".equals(mirrors[i])) {
                        logger.warn("Replace mirror 'https://mirror.gravit.pro/' to 'https://mirror.gravit.pro/5.2.x/'. If you really need to use original url, use '-Dlaunchserver.config.disableUpdateMirror=true'");
                        mirrors[i] = "https://mirror.gravit.pro/5.2.x/";
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
            protectHandler.checkLaunchServerLicense();
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
                    if (component instanceof AutoCloseable) {
                        try {
                            ((AutoCloseable) component).close();
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
    }

    public static class ExeConf {
        public boolean enabled;
        public boolean setMaxVersion;
        public String maxVersion;
        public String minVersion = "1.8.0";
        public String supportURL = null;
        public String downloadUrl = Launch4JTask.DOWNLOAD_URL;
        public String productName;
        public String productVer;
        public String fileDesc;
        public String fileVer;
        public String internalName;
        public String copyright;
        public String trademarks;

        public String txtFileVersion;
        public String txtProductVersion;
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
    }

    public static class NettyUpdatesBind {
        public String url;
        public boolean zip;
    }

    public static class LauncherConf {
        public String guardType;
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
        @Deprecated
        public boolean sendExceptionEnabled;
        public boolean ipForwarding;
        public boolean disableWebApiInterface;
        public boolean showHiddenFiles;
        public String launcherURL;
        public String downloadURL;
        public String launcherEXEURL;
        public String address;
        public Map<String, LaunchServerConfig.NettyUpdatesBind> bindings = new HashMap<>();
        public NettyPerformanceConfig performance;
        public NettyBindAddress[] binds;
        public LogLevel logLevel = LogLevel.DEBUG;
    }

    public static class NettyPerformanceConfig {
        public boolean usingEpoll;
        public int bossThread;
        public int workerThread;
        public int schedulerThread;
        public long sessionLifetimeMs = 24 * 60 * 60 * 1000;
        public int maxWebSocketRequestBytes = 1024 * 1024;
    }

    public static class NettyBindAddress {
        public String address;
        public int port;

        public NettyBindAddress(String address, int port) {
            this.address = address;
            this.port = port;
        }
    }
}
