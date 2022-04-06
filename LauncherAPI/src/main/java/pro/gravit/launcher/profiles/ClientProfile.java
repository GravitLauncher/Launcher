package pro.gravit.launcher.profiles;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.hasher.FileNameMatcher;
import pro.gravit.launcher.profiles.optional.OptionalDepend;
import pro.gravit.launcher.profiles.optional.OptionalFile;
import pro.gravit.launcher.profiles.optional.triggers.OptionalTrigger;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

public final class ClientProfile implements Comparable<ClientProfile> {
    private static final FileNameMatcher ASSET_MATCHER = new FileNameMatcher(
            new String[0], new String[]{"indexes", "objects"}, new String[0]);

    @LauncherNetworkAPI
    private String title;
    @LauncherNetworkAPI
    private UUID uuid;
    @LauncherNetworkAPI
    private String version;
    @LauncherNetworkAPI
    private String info;
    @LauncherNetworkAPI
    private String dir;
    @LauncherNetworkAPI
    private int sortIndex;
    @LauncherNetworkAPI
    private String assetIndex;
    @LauncherNetworkAPI
    private String assetDir;
    //  Updater and client watch service
    @LauncherNetworkAPI
    private List<String> update;
    @LauncherNetworkAPI
    private List<String> updateExclusions;
    @LauncherNetworkAPI
    private List<String> updateShared;
    @LauncherNetworkAPI
    private List<String> updateVerify;
    @LauncherNetworkAPI
    private Set<OptionalFile> updateOptional;
    @LauncherNetworkAPI
    private List<String> jvmArgs;
    @LauncherNetworkAPI
    private List<String> classPath;
    @LauncherNetworkAPI
    private List<String> modulePath = new ArrayList<>();
    @LauncherNetworkAPI
    private List<String> modules = new ArrayList<>();
    @LauncherNetworkAPI
    private List<String> altClassPath;
    @LauncherNetworkAPI
    private List<String> clientArgs;
    @LauncherNetworkAPI
    private List<String> compatClasses;
    @LauncherNetworkAPI
    private Map<String, String> properties;
    @LauncherNetworkAPI
    private List<ServerProfile> servers;
    @LauncherNetworkAPI
    private SecurityManagerConfig securityManagerConfig;
    @LauncherNetworkAPI
    private ClassLoaderConfig classLoaderConfig;
    @LauncherNetworkAPI
    private SignedClientConfig signedClientConfig;
    @LauncherNetworkAPI
    private RuntimeInClientConfig runtimeInClientConfig;
    @LauncherNetworkAPI
    private int recommendJavaVersion = 8;
    @LauncherNetworkAPI
    private int minJavaVersion = 8;
    @LauncherNetworkAPI
    private int maxJavaVersion = 999;
    @LauncherNetworkAPI
    private boolean warnMissJavaVersion = true;
    @LauncherNetworkAPI
    private ProfileDefaultSettings settings = new ProfileDefaultSettings();
    @LauncherNetworkAPI
    private boolean updateFastCheck;
    @LauncherNetworkAPI
    private boolean limited;
    // Client launcher
    @LauncherNetworkAPI
    private String mainClass;

    public ClientProfile() {
        update = new ArrayList<>();
        updateExclusions = new ArrayList<>();
        updateShared = new ArrayList<>();
        updateVerify = new ArrayList<>();
        updateOptional = new HashSet<>();
        jvmArgs = new ArrayList<>();
        classPath = new ArrayList<>();
        modulePath = new ArrayList<>();
        altClassPath = new ArrayList<>();
        clientArgs = new ArrayList<>();
        compatClasses = new ArrayList<>();
        properties = new HashMap<>();
        servers = new ArrayList<>(1);
        securityManagerConfig = SecurityManagerConfig.CLIENT;
        classLoaderConfig = ClassLoaderConfig.LAUNCHER;
        signedClientConfig = SignedClientConfig.NONE;
        runtimeInClientConfig = RuntimeInClientConfig.NONE;
    }

    public ClientProfile(List<String> update, List<String> updateExclusions, List<String> updateShared, List<String> updateVerify, Set<OptionalFile> updateOptional, List<String> jvmArgs, List<String> classPath, List<String> modulePath, List<String> modules, List<String> altClassPath, List<String> clientArgs, List<String> compatClasses, Map<String, String> properties, List<ServerProfile> servers, SecurityManagerConfig securityManagerConfig, ClassLoaderConfig classLoaderConfig, SignedClientConfig signedClientConfig, RuntimeInClientConfig runtimeInClientConfig, String version, String assetIndex, String dir, String assetDir, int recommendJavaVersion, int minJavaVersion, int maxJavaVersion, boolean warnMissJavaVersion, ProfileDefaultSettings settings, int sortIndex, UUID uuid, String title, String info, boolean updateFastCheck, String mainClass) {
        this.update = update;
        this.updateExclusions = updateExclusions;
        this.updateShared = updateShared;
        this.updateVerify = updateVerify;
        this.updateOptional = updateOptional;
        this.jvmArgs = jvmArgs;
        this.classPath = classPath;
        this.modulePath = modulePath;
        this.modules = modules;
        this.altClassPath = altClassPath;
        this.clientArgs = clientArgs;
        this.compatClasses = compatClasses;
        this.properties = properties;
        this.servers = servers;
        this.securityManagerConfig = securityManagerConfig;
        this.classLoaderConfig = classLoaderConfig;
        this.signedClientConfig = signedClientConfig;
        this.runtimeInClientConfig = runtimeInClientConfig;
        this.version = version;
        this.assetIndex = assetIndex;
        this.dir = dir;
        this.assetDir = assetDir;
        this.recommendJavaVersion = recommendJavaVersion;
        this.minJavaVersion = minJavaVersion;
        this.maxJavaVersion = maxJavaVersion;
        this.warnMissJavaVersion = warnMissJavaVersion;
        this.settings = settings;
        this.sortIndex = sortIndex;
        this.uuid = uuid;
        this.title = title;
        this.info = info;
        this.updateFastCheck = updateFastCheck;
        this.mainClass = mainClass;
    }

    public ServerProfile getDefaultServerProfile() {
        for (ServerProfile profile : servers) {
            if (profile.isDefault) return profile;
        }
        return null;
    }

    @Override
    public int compareTo(ClientProfile o) {
        return Integer.compare(getSortIndex(), o.getSortIndex());
    }

    public String getAssetIndex() {
        return assetIndex;
    }

    public FileNameMatcher getAssetUpdateMatcher() {
        return getVersion().compareTo(Version.MC1710) >= 0 ? ASSET_MATCHER : null;
    }

    public String[] getClassPath() {
        return classPath.toArray(new String[0]);
    }

    public List<String> getModulePath() {
        return Collections.unmodifiableList(modulePath);
    }

    public List<String> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public String[] getAlternativeClassPath() {
        return altClassPath.toArray(new String[0]);
    }

    public String[] getClientArgs() {
        return clientArgs.toArray(new String[0]);
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getAssetDir() {
        return assetDir;
    }

    public List<String> getUpdateExclusions() {
        return Collections.unmodifiableList(updateExclusions);
    }

    public FileNameMatcher getClientUpdateMatcher(/*boolean excludeOptional*/) {
        String[] updateArray = update.toArray(new String[0]);
        String[] verifyArray = updateVerify.toArray(new String[0]);
        List<String> excludeList;
        //if(excludeOptional)
        //{
        //    excludeList = new ArrayList<>();
        //    excludeList.addAll(updateExclusions);
        //    excludeList.addAll(updateOptional);
        //}
        //else
        excludeList = updateExclusions;
        String[] exclusionsArray = excludeList.toArray(new String[0]);
        return new FileNameMatcher(updateArray, verifyArray, exclusionsArray);
    }

    public String[] getJvmArgs() {
        return jvmArgs.toArray(new String[0]);
    }

    public String getMainClass() {
        return mainClass;
    }

    public List<ServerProfile> getServers() {
        return servers;
    }

    public String getServerAddress() {
        ServerProfile profile = getDefaultServerProfile();
        return profile == null ? "localhost" : profile.serverAddress;
    }

    public Set<OptionalFile> getOptional() {
        return updateOptional;
    }

    public int getRecommendJavaVersion() {
        return recommendJavaVersion;
    }

    public int getMinJavaVersion() {
        return minJavaVersion;
    }

    public int getMaxJavaVersion() {
        return maxJavaVersion;
    }

    public boolean isWarnMissJavaVersion() {
        return warnMissJavaVersion;
    }

    public ProfileDefaultSettings getSettings() {
        return settings;
    }

    public void updateOptionalGraph() {
        for (OptionalFile file : updateOptional) {
            if (file.dependenciesFile != null) {
                file.dependencies = new OptionalFile[file.dependenciesFile.length];
                for (int i = 0; i < file.dependenciesFile.length; ++i) {
                    file.dependencies[i] = getOptionalFile(file.dependenciesFile[i].name);
                }
            }
            if (file.conflictFile != null) {
                file.conflict = new OptionalFile[file.conflictFile.length];
                for (int i = 0; i < file.conflictFile.length; ++i) {
                    file.conflict[i] = getOptionalFile(file.conflictFile[i].name);
                }
            }
        }
    }

    public OptionalFile getOptionalFile(String file) {
        for (OptionalFile f : updateOptional)
            if (f.name.equals(file)) return f;
        return null;
    }

    public Collection<String> getShared() {
        return updateShared;
    }

    public int getServerPort() {
        ServerProfile profile = getDefaultServerProfile();
        return profile == null ? 25565 : profile.serverPort;
    }

    public int getSortIndex() {
        return sortIndex;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Version getVersion() {
        return Version.byName(version);
    }

    public void setVersion(Version version) {
        this.version = version.name;
    }

    public boolean isUpdateFastCheck() {
        return updateFastCheck;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", title, uuid);
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public void verify() {
        // Version
        getVersion();
        IOHelper.verifyFileName(getAssetIndex());

        // Client
        VerifyHelper.verify(getTitle(), VerifyHelper.NOT_EMPTY, "Profile title can't be empty");
        VerifyHelper.verify(getInfo(), VerifyHelper.NOT_EMPTY, "Profile info can't be empty");

        // Client launcher
        VerifyHelper.verify(getTitle(), VerifyHelper.NOT_EMPTY, "Main class can't be empty");
        if (getUUID() == null) {
            throw new IllegalArgumentException("Profile UUID can't be null");
        }
        for (String s : update) {
            if (s == null) throw new IllegalArgumentException("Found null entry in update");
        }
        for (String s : updateVerify) {
            if (s == null) throw new IllegalArgumentException("Found null entry in updateVerify");
        }
        for (String s : updateExclusions) {
            if (s == null) throw new IllegalArgumentException("Found null entry in updateExclusions");
        }

        for (String s : classPath) {
            if (s == null) throw new IllegalArgumentException("Found null entry in classPath");
        }
        for (String s : jvmArgs) {
            if (s == null) throw new IllegalArgumentException("Found null entry in jvmArgs");
        }
        for (String s : clientArgs) {
            if (s == null) throw new IllegalArgumentException("Found null entry in clientArgs");
        }
        for (String s : compatClasses) {
            if (s == null) throw new IllegalArgumentException("Found null entry in compatClasses");
        }
        for (OptionalFile f : updateOptional) {
            if (f == null) throw new IllegalArgumentException("Found null entry in updateOptional");
            if (f.name == null) throw new IllegalArgumentException("Optional: name must not be null");
            if (f.conflictFile != null) for (OptionalDepend s : f.conflictFile) {
                if (s == null)
                    throw new IllegalArgumentException(String.format("Found null entry in updateOptional.%s.conflictFile", f.name));
            }
            if (f.dependenciesFile != null) for (OptionalDepend s : f.dependenciesFile) {
                if (s == null)
                    throw new IllegalArgumentException(String.format("Found null entry in updateOptional.%s.dependenciesFile", f.name));
            }
            if (f.triggersList != null) {
                for (OptionalTrigger trigger : f.triggersList) {
                    if (trigger == null)
                        throw new IllegalArgumentException(String.format("Found null entry in updateOptional.%s.triggers", f.name));
                }
            }
        }
    }

    public String getProperty(String name) {
        return properties.get(name);
    }

    public void putProperty(String name, String value) {
        properties.put(name, value);
    }

    public boolean containsProperty(String name) {
        return properties.containsKey(name);
    }

    public void clearProperties() {
        properties.clear();
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public List<String> getCompatClasses() {
        return Collections.unmodifiableList(compatClasses);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientProfile profile = (ClientProfile) o;
        return Objects.equals(uuid, profile.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    public ClassLoaderConfig getClassLoaderConfig() {
        return classLoaderConfig;
    }

    public void setClassLoaderConfig(ClassLoaderConfig classLoaderConfig) {
        this.classLoaderConfig = classLoaderConfig;
    }

    public RuntimeInClientConfig getRuntimeInClientConfig() {
        return runtimeInClientConfig;
    }

    public void setRuntimeInClientConfig(RuntimeInClientConfig runtimeInClientConfig) {
        this.runtimeInClientConfig = runtimeInClientConfig;
    }

    public boolean isLimited() {
        return limited;
    }

    public enum Version {
        MC125("1.2.5", 29),
        MC147("1.4.7", 51),
        MC152("1.5.2", 61),
        MC164("1.6.4", 78),
        MC172("1.7.2", 4),
        MC1710("1.7.10", 5),
        MC189("1.8.9", 47),
        MC19("1.9", 107),
        MC192("1.9.2", 109),
        MC194("1.9.4", 110),
        MC1102("1.10.2", 210),
        MC111("1.11", 315),
        MC1112("1.11.2", 316),
        MC112("1.12", 335),
        MC1121("1.12.1", 338),
        MC1122("1.12.2", 340),
        MC113("1.13", 393),
        MC1131("1.13.1", 401),
        MC1132("1.13.2", 402),
        MC114("1.14", 477),
        MC1141("1.14.1", 480),
        MC1142("1.14.2", 485),
        MC1143("1.14.3", 490),
        MC1144("1.14.4", 498),
        MC115("1.15", 573),
        MC1151("1.15.1", 575),
        MC1152("1.15.2", 578),
        MC1161("1.16.1", 736),
        MC1162("1.16.2", 751),
        MC1163("1.16.3", 753),
        MC1164("1.16.4", 754),
        MC1165("1.16.5", 754),
        MC117("1.17", 755),
        MC1171("1.17.1", 756),
        MC118("1.18", 757),
        MC1181("1.18.1", 757),
        MC1182("1.18.2", 758);
        private static final Map<String, Version> VERSIONS;

        static {
            Version[] versionsValues = values();
            VERSIONS = new HashMap<>(versionsValues.length);
            for (Version version : versionsValues)
                VERSIONS.put(version.name, version);
        }

        public final String name;
        public final int protocol;

        Version(String name, int protocol) {
            this.name = name;
            this.protocol = protocol;
        }

        public static Version byName(String name) {
            return VerifyHelper.getMapValue(VERSIONS, name, String.format("Unknown client version: '%s'", name));
        }

        @Override
        public String toString() {
            return "Minecraft " + name;
        }
    }

    public enum SecurityManagerConfig {
        NONE, CLIENT, LAUNCHER, MIXED
    }

    public enum ClassLoaderConfig {
        AGENT, LAUNCHER, MODULE, SYSTEM_ARGS
    }

    public enum SignedClientConfig {
        NONE, SIGNED
    }

    public enum RuntimeInClientConfig {
        NONE, BASIC, FULL
    }

    @FunctionalInterface
    public interface pushOptionalClassPathCallback {
        void run(String[] opt) throws IOException;
    }

    public static class ServerProfile {
        public String name;
        public String serverAddress;
        public int serverPort;
        public boolean isDefault = true;
        public boolean socketPing = true;

        public ServerProfile() {
        }

        public ServerProfile(String name, String serverAddress, int serverPort) {
            this.name = name;
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
        }

        public ServerProfile(String name, String serverAddress, int serverPort, boolean isDefault) {
            this.name = name;
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
            this.isDefault = isDefault;
        }

        public InetSocketAddress toSocketAddress() {
            return InetSocketAddress.createUnresolved(serverAddress, serverPort);
        }
    }

    public static class ProfileDefaultSettings {
        public int ram;
        public boolean autoEnter;
        public boolean fullScreen;
    }
}
