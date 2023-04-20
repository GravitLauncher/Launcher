package pro.gravit.launcher.profiles;

import pro.gravit.launcher.profiles.optional.OptionalFile;

import java.util.*;

public class ClientProfileBuilder {
    private List<String> update = new ArrayList<>();
    private List<String> updateExclusions = new ArrayList<>();
    private List<String> updateShared = new ArrayList<>();
    private List<String> updateVerify = new ArrayList<>();
    private Set<OptionalFile> updateOptional = new HashSet<>();
    private List<String> jvmArgs = new ArrayList<>();
    private List<String> classPath = new ArrayList<>();
    private List<String> modulePath = new ArrayList<>();
    private List<String> modules = new ArrayList<>();
    private List<String> altClassPath = new ArrayList<>();
    private List<String> clientArgs = new ArrayList<>();
    private List<String> compatClasses = new ArrayList<>();
    private Map<String, String> properties = new HashMap<>();
    private List<ClientProfile.ServerProfile> servers = new ArrayList<>();
    private ClientProfile.ClassLoaderConfig classLoaderConfig = ClientProfile.ClassLoaderConfig.LAUNCHER;
    private List<ClientProfile.CompatibilityFlags> flags = new ArrayList<>();
    private ClientProfile.Version version;
    private String assetIndex;
    private String dir;
    private String assetDir;
    private int recommendJavaVersion = 8;
    private int minJavaVersion = 8;
    private int maxJavaVersion = 999;
    private ClientProfile.ProfileDefaultSettings settings = new ClientProfile.ProfileDefaultSettings();
    private int sortIndex;
    private UUID uuid;
    private String title;
    private String info;
    private String mainClass;

    public void setUpdate(List<String> update) {
        this.update = update;
    }

    public ClientProfileBuilder setUpdateExclusions(List<String> updateExclusions) {
        this.updateExclusions = updateExclusions;
        return this;
    }

    public ClientProfileBuilder setUpdateShared(List<String> updateShared) {
        this.updateShared = updateShared;
        return this;
    }

    public void setUpdateVerify(List<String> updateVerify) {
        this.updateVerify = updateVerify;
    }

    public void setUpdateOptional(Set<OptionalFile> updateOptional) {
        this.updateOptional = updateOptional;
    }

    public void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public void setClassPath(List<String> classPath) {
        this.classPath = classPath;
    }

    public void setAltClassPath(List<String> altClassPath) {
        this.altClassPath = altClassPath;
    }

    public void setClientArgs(List<String> clientArgs) {
        this.clientArgs = clientArgs;
    }

    public ClientProfileBuilder setCompatClasses(List<String> compatClasses) {
        this.compatClasses = compatClasses;
        return this;
    }

    public ClientProfileBuilder setProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public void setServers(List<ClientProfile.ServerProfile> servers) {
        this.servers = servers;
    }

    public void setClassLoaderConfig(ClientProfile.ClassLoaderConfig classLoaderConfig) {
        this.classLoaderConfig = classLoaderConfig;
    }

    public void setVersion(ClientProfile.Version version) {
        this.version = version;
    }

    public void setAssetIndex(String assetIndex) {
        this.assetIndex = assetIndex;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public void setAssetDir(String assetDir) {
        this.assetDir = assetDir;
    }

    public void setRecommendJavaVersion(int recommendJavaVersion) {
        this.recommendJavaVersion = recommendJavaVersion;
    }

    public ClientProfileBuilder setModulePath(List<String> modulePath) {
        this.modulePath = modulePath;
        return this;
    }

    public ClientProfileBuilder setModules(List<String> modules) {
        this.modules = modules;
        return this;
    }

    public void setMinJavaVersion(int minJavaVersion) {
        this.minJavaVersion = minJavaVersion;
    }

    public void setMaxJavaVersion(int maxJavaVersion) {
        this.maxJavaVersion = maxJavaVersion;
    }

    public ClientProfileBuilder setSettings(ClientProfile.ProfileDefaultSettings settings) {
        this.settings = settings;
        return this;
    }

    public ClientProfileBuilder setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
        return this;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public ClientProfileBuilder setFlags(List<ClientProfile.CompatibilityFlags> flags) {
        this.flags = flags;
        return this;
    }

    public ClientProfile createClientProfile() {
        return new ClientProfile(update, updateExclusions, updateShared, updateVerify, updateOptional, jvmArgs, classPath, modulePath, modules, altClassPath, clientArgs, compatClasses, properties, servers, classLoaderConfig, flags, version, assetIndex, dir, assetDir, recommendJavaVersion, minJavaVersion, maxJavaVersion, settings, sortIndex, uuid, title, info, mainClass);
    }
}