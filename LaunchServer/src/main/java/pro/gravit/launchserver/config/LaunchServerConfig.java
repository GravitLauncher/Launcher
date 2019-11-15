package pro.gravit.launchserver.config;

import io.netty.channel.epoll.Epoll;
import io.netty.handler.logging.LogLevel;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.handler.MemoryAuthHandler;
import pro.gravit.launchserver.auth.hwid.AcceptHWIDHandler;
import pro.gravit.launchserver.auth.hwid.HWIDHandler;
import pro.gravit.launchserver.auth.permissions.DefaultPermissionsHandler;
import pro.gravit.launchserver.auth.permissions.JsonFilePermissionsHandler;
import pro.gravit.launchserver.auth.permissions.PermissionsHandler;
import pro.gravit.launchserver.auth.protect.ProtectHandler;
import pro.gravit.launchserver.auth.protect.StdProtectHandler;
import pro.gravit.launchserver.auth.provider.RejectAuthProvider;
import pro.gravit.launchserver.auth.texture.RequestTextureProvider;
import pro.gravit.launchserver.components.AuthLimiterComponent;
import pro.gravit.launchserver.components.Component;
import pro.gravit.launchserver.components.RegLimiterComponent;
import pro.gravit.launchserver.dao.provider.DaoProvider;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class LaunchServerConfig {
    private transient LaunchServer server = null;

    public LaunchServerConfig setLaunchServer(LaunchServer server) {
        this.server = server;
        return this;
    }

    public String projectName;

    public String[] mirrors;

    public String binaryName;

    public final boolean copyBinaries = true;

    public LauncherConfig.LauncherEnvironment env;

    // Handlers & Providers

    public AuthProviderPair[] auth;

    public DaoProvider dao;

    private transient AuthProviderPair authDefault;

    public AuthProviderPair getAuthProviderPair(String name) {
        for (AuthProviderPair pair : auth) {
            if (pair.name.equals(name)) return pair;
        }
        return null;
    }

    public ProtectHandler protectHandler;

    public PermissionsHandler permissionsHandler;

    public AuthProviderPair getAuthProviderPair() {
        if (authDefault != null) return authDefault;
        for (AuthProviderPair pair : auth) {
            if (pair.isDefault) {
                authDefault = pair;
                return pair;
            }
        }
        throw new IllegalStateException("Default AuthProviderPair not found");
    }

    public HWIDHandler hwidHandler;

    public Map<String, Component> components;

    public ExeConf launch4j;
    public NettyConfig netty;

    public String whitelistRejectString;
    public LauncherConf launcher;
    public CertificateConf certificate;
    public JarSignerConf sign;

    public String startScript;

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
        if (auth == null || auth[0] == null) {
            throw new NullPointerException("AuthHandler must not be null");
        }
        boolean isOneDefault = false;
        for (AuthProviderPair pair : auth) {
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
        if (permissionsHandler == null) {
            throw new NullPointerException("PermissionsHandler must not be null");
        }
        if (env == null) {
            throw new NullPointerException("Env must not be null");
        }
        if (netty == null) {
            throw new NullPointerException("Netty must not be null");
        }
    }

    public void init(LaunchServer.ReloadType type) {
        Launcher.applyLauncherEnv(env);
        for (AuthProviderPair provider : auth) {
            provider.init(server);
        }
        permissionsHandler.init(server);
        hwidHandler.init();
        if (dao != null)
            dao.init(server);
        if (protectHandler != null) {
            protectHandler.checkLaunchServerLicense();
        }
        if (components != null) {
            components.forEach((k, v) -> server.registerObject("component.".concat(k), v));
        }
        server.registerObject("permissionsHandler", permissionsHandler);
        server.registerObject("hwidHandler", hwidHandler);
        if (!type.equals(LaunchServer.ReloadType.NO_AUTH)) {
            for (AuthProviderPair pair : auth) {
                server.registerObject("auth.".concat(pair.name).concat(".provider"), pair.provider);
                server.registerObject("auth.".concat(pair.name).concat(".handler"), pair.handler);
                server.registerObject("auth.".concat(pair.name).concat(".texture"), pair.textureProvider);
            }
        }


        Arrays.stream(mirrors).forEach(server.mirrorManager::addMirror);
    }

    public void close(LaunchServer.ReloadType type) {
        try {
            server.unregisterObject("permissionsHandler", permissionsHandler);
            server.unregisterObject("hwidHandler", hwidHandler);
            if (!type.equals(LaunchServer.ReloadType.NO_AUTH)) {
                for (AuthProviderPair pair : auth) {
                    server.unregisterObject("auth.".concat(pair.name).concat(".provider"), pair.provider);
                    server.unregisterObject("auth.".concat(pair.name).concat(".handler"), pair.handler);
                    server.unregisterObject("auth.".concat(pair.name).concat(".texture"), pair.textureProvider);
                }
            }
            if (type.equals(LaunchServer.ReloadType.FULL)) {
                components.forEach((k, component) -> {
                    server.unregisterObject("component.".concat(k), component);
                    if (component instanceof AutoCloseable) {
                        try {
                            ((AutoCloseable) component).close();
                        } catch (Exception e) {
                            LogHelper.error(e);
                        }
                    }
                });
            }
        } catch (Exception e) {
            LogHelper.error(e);
        }
        try {
            for (AuthProviderPair p : auth) p.close();
        } catch (IOException e) {
            LogHelper.error(e);
        }
        try {
            hwidHandler.close();
        } catch (Exception e) {
            LogHelper.error(e);
        }
        try {
            permissionsHandler.close();
        } catch (Exception e) {
            LogHelper.error(e);
        }
    }

    public static class ExeConf {
        public boolean enabled;
        public boolean setMaxVersion;
        public String maxVersion;
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

    public static class CertificateConf {
        public boolean enabled;
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
        public boolean attachLibraryBeforeProGuard;
        public boolean compress;
        public boolean warningMissArchJava;
        public boolean enabledProGuard;
        public boolean stripLineNumbers;
        public boolean deleteTempFiles;
        public boolean proguardGenMappings;
    }

    public static class NettyConfig {
        public boolean fileServerEnabled;
        public boolean sendExceptionEnabled;
        public boolean ipForwarding;
        public boolean showHiddenFiles;
        public String launcherURL;
        public String downloadURL;
        public String launcherEXEURL;
        public String address;
        public final Map<String, LaunchServerConfig.NettyUpdatesBind> bindings = new HashMap<>();
        public NettyPerformanceConfig performance;
        public NettyBindAddress[] binds;
        public final LogLevel logLevel = LogLevel.DEBUG;
    }

    public static class NettyPerformanceConfig {
        public boolean usingEpoll;
        public int bossThread;
        public int workerThread;
    }

    public static class NettyBindAddress {
        public final String address;
        public final int port;

        public NettyBindAddress(String address, int port) {
            this.address = address;
            this.port = port;
        }
    }

    public static LaunchServerConfig getDefault(LaunchServer.LaunchServerEnv env) {
        LaunchServerConfig newConfig = new LaunchServerConfig();
        newConfig.mirrors = new String[]{"https://mirror.gravit.pro/"};
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
        newConfig.hwidHandler = new AcceptHWIDHandler();
        newConfig.auth = new AuthProviderPair[]{new AuthProviderPair(new RejectAuthProvider("Настройте authProvider"),
                new MemoryAuthHandler(),
                new RequestTextureProvider("http://example.com/skins/%username%.png", "http://example.com/cloaks/%username%.png")
                , "std")};
        newConfig.auth[0].displayName = "Default";
        newConfig.protectHandler = new StdProtectHandler();
        if (env.equals(LaunchServer.LaunchServerEnv.TEST))
            newConfig.permissionsHandler = new DefaultPermissionsHandler();
        else
            newConfig.permissionsHandler = new JsonFilePermissionsHandler();
        newConfig.binaryName = "Launcher";
        newConfig.whitelistRejectString = "Вас нет в белом списке";

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

        newConfig.launcher = new LauncherConf();
        newConfig.launcher.guardType = "no";
        newConfig.launcher.compress = true;
        newConfig.launcher.warningMissArchJava = true;
        newConfig.launcher.attachLibraryBeforeProGuard = false;
        newConfig.launcher.deleteTempFiles = true;
        newConfig.launcher.enabledProGuard = true;
        newConfig.launcher.stripLineNumbers = true;
        newConfig.launcher.proguardGenMappings = true;

        newConfig.certificate = new LaunchServerConfig.CertificateConf();
        newConfig.certificate.enabled = false;
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
        newConfig.netty.sendExceptionEnabled = true;
        return newConfig;
    }
}
