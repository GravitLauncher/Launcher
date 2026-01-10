package pro.gravit.launchserver.config;

import io.netty.handler.logging.LogLevel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.profiles.LocalProfilesProvider;
import pro.gravit.launchserver.auth.profiles.ProfilesProvider;
import pro.gravit.launchserver.auth.updates.LocalUpdatesProvider;
import pro.gravit.launchserver.auth.updates.UpdatesProvider;
import pro.gravit.launchserver.components.Component;
import pro.gravit.launchserver.components.ProGuardComponent;

import java.util.*;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class LaunchServerConfig {
    private final static List<String> oldMirrorList = List.of("https://mirror.gravit.pro/5.2.x/", "https://mirror.gravit.pro/5.3.x/",
            "https://mirror.gravitlauncher.com/5.2.x/", "https://mirror.gravitlauncher.com/5.3.x/", "https://mirror.gravitlauncher.com/5.4.x/",
            "https://mirror.gravitlauncher.com/5.5.x/", "https://mirror.gravitlauncher.com/5.6.x/");
    private transient final Logger logger = LogManager.getLogger();
    public String projectName;
    public String[] mirrors;
    public LauncherConfig.LauncherEnvironment env;
    // Handlers & Providers
    public Map<String, Component> components;
    public ProfilesProvider profilesProvider = new LocalProfilesProvider();
    public UpdatesProvider updatesProvider = new LocalUpdatesProvider();
    public NettyConfig netty;
    public LauncherConf launcher;
    public JarSignerConf sign;
    private transient LaunchServer server = null;

    public static LaunchServerConfig getDefault(LaunchServer.LaunchServerEnv env) {
        LaunchServerConfig newConfig = new LaunchServerConfig();
        newConfig.mirrors = new String[]{"https://mirror.gravitlauncher.com/5.7.x/"};
        newConfig.env = LauncherConfig.LauncherEnvironment.STD;

        newConfig.netty = new NettyConfig();
        newConfig.netty.fileServerEnabled = true;
        newConfig.netty.binds = new NettyBindAddress[]{new NettyBindAddress("0.0.0.0", 9274)};

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
                        mirrors[i] = "https://mirror.gravitlauncher.com/5.7.x/";
                    }
                }
            }
        }
    }

    public void init(LaunchServer.ReloadType type) {
        Launcher.applyLauncherEnv(env);
        if(profilesProvider != null) {
            server.registerObject("profileProvider", profilesProvider);
            profilesProvider.init(server);
        }
        if(updatesProvider != null) {
            server.registerObject("updatesProvider", updatesProvider);
            updatesProvider.init(server);
        }
        if (components != null) {
            components.forEach((k, v) -> server.registerObject("component.".concat(k), v));
        }
        Arrays.stream(mirrors).forEach(server.mirrorManager::addMirror);
    }

    public void close(LaunchServer.ReloadType type) {
        try {
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
        if(profilesProvider != null) {
            server.unregisterObject("profilesProvider", profilesProvider);
            profilesProvider.close();
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
        public Map<String, String> customJavaDownload = new HashMap<>();
        public boolean forceUseCustomJava;
        public int memoryLimit = 256;
    }

    public static class NettyConfig {
        public boolean fileServerEnabled;
        public boolean ipForwarding;
        public boolean disableWebApiInterface;
        public boolean showHiddenFiles;
        public boolean sendProfileUpdatesEvent = true;
        public String downloadURL;
        public String address;
        public Map<String, LaunchServerConfig.NettyUpdatesBind> bindings = new HashMap<>();

        public NettySecurityConfig security = new NettySecurityConfig();
        public NettyBindAddress[] binds;
        public LogLevel logLevel = LogLevel.DEBUG;
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
