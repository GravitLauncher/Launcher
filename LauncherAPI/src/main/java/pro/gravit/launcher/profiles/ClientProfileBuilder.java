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
    private ClientProfile.SecurityManagerConfig securityManagerConfig = ClientProfile.SecurityManagerConfig.LAUNCHER;
    private ClientProfile.ClassLoaderConfig classLoaderConfig = ClientProfile.ClassLoaderConfig.LAUNCHER;
    private ClientProfile.SignedClientConfig signedClientConfig = ClientProfile.SignedClientConfig.NONE;
    private ClientProfile.RuntimeInClientConfig runtimeInClientConfig = ClientProfile.RuntimeInClientConfig.NONE;
    private String version;
    private String assetIndex;
    private String dir;
    private String assetDir;
    private int recommendJavaVersion = 8;
    private int minJavaVersion = 8;
    private int maxJavaVersion = 999;
    private boolean warnMissJavaVersion = true;
    private ClientProfile.ProfileDefaultSettings settings = new ClientProfile.ProfileDefaultSettings();
    private int sortIndex;
    private UUID uuid;
    private String title;
    private String info;
    private boolean updateFastCheck = true;
    private String mainClass;

    public ClientProfileBuilder setUpdate(List<String> update) {
        this.update = update;
        return this;
    }

    public ClientProfileBuilder setUpdateExclusions(List<String> updateExclusions) {
        this.updateExclusions = updateExclusions;
        return this;
    }

    public ClientProfileBuilder setUpdateShared(List<String> updateShared) {
        this.updateShared = updateShared;
        return this;
    }

    public ClientProfileBuilder setUpdateVerify(List<String> updateVerify) {
        this.updateVerify = updateVerify;
        return this;
    }

    public ClientProfileBuilder setUpdateOptional(Set<OptionalFile> updateOptional) {
        this.updateOptional = updateOptional;
        return this;
    }

    public ClientProfileBuilder setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
        return this;
    }

    public ClientProfileBuilder setClassPath(List<String> classPath) {
        this.classPath = classPath;
        return this;
    }

    public ClientProfileBuilder setAltClassPath(List<String> altClassPath) {
        this.altClassPath = altClassPath;
        return this;
    }

    public ClientProfileBuilder setClientArgs(List<String> clientArgs) {
        this.clientArgs = clientArgs;
        return this;
    }

    public ClientProfileBuilder setCompatClasses(List<String> compatClasses) {
        this.compatClasses = compatClasses;
        return this;
    }

    public ClientProfileBuilder setProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public ClientProfileBuilder setServers(List<ClientProfile.ServerProfile> servers) {
        this.servers = servers;
        return this;
    }

    public ClientProfileBuilder setSecurityManagerConfig(ClientProfile.SecurityManagerConfig securityManagerConfig) {
        this.securityManagerConfig = securityManagerConfig;
        return this;
    }

    public ClientProfileBuilder setClassLoaderConfig(ClientProfile.ClassLoaderConfig classLoaderConfig) {
        this.classLoaderConfig = classLoaderConfig;
        return this;
    }

    public ClientProfileBuilder setSignedClientConfig(ClientProfile.SignedClientConfig signedClientConfig) {
        this.signedClientConfig = signedClientConfig;
        return this;
    }

    public ClientProfileBuilder setRuntimeInClientConfig(ClientProfile.RuntimeInClientConfig runtimeInClientConfig) {
        this.runtimeInClientConfig = runtimeInClientConfig;
        return this;
    }

    public ClientProfileBuilder setVersion(String version) {
        this.version = version;
        return this;
    }

    public ClientProfileBuilder setAssetIndex(String assetIndex) {
        this.assetIndex = assetIndex;
        return this;
    }

    public ClientProfileBuilder setDir(String dir) {
        this.dir = dir;
        return this;
    }

    public ClientProfileBuilder setAssetDir(String assetDir) {
        this.assetDir = assetDir;
        return this;
    }

    public ClientProfileBuilder setRecommendJavaVersion(int recommendJavaVersion) {
        this.recommendJavaVersion = recommendJavaVersion;
        return this;
    }

    public ClientProfileBuilder setModulePath(List<String> modulePath) {
        this.modulePath = modulePath;
        return this;
    }

    public ClientProfileBuilder setModules(List<String> modules) {
        this.modules = modules;
        return this;
    }

    public ClientProfileBuilder setMinJavaVersion(int minJavaVersion) {
        this.minJavaVersion = minJavaVersion;
        return this;
    }

    public ClientProfileBuilder setMaxJavaVersion(int maxJavaVersion) {
        this.maxJavaVersion = maxJavaVersion;
        return this;
    }

    public ClientProfileBuilder setWarnMissJavaVersion(boolean warnMissJavaVersion) {
        this.warnMissJavaVersion = warnMissJavaVersion;
        return this;
    }

    public ClientProfileBuilder setSettings(ClientProfile.ProfileDefaultSettings settings) {
        this.settings = settings;
        return this;
    }

    public ClientProfileBuilder setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
        return this;
    }

    public ClientProfileBuilder setUuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public ClientProfileBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public ClientProfileBuilder setInfo(String info) {
        this.info = info;
        return this;
    }

    public ClientProfileBuilder setUpdateFastCheck(boolean updateFastCheck) {
        this.updateFastCheck = updateFastCheck;
        return this;
    }

    public ClientProfileBuilder setMainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public ClientProfile createClientProfile() {
        return new ClientProfile(update, updateExclusions, updateShared, updateVerify, updateOptional, jvmArgs, classPath, modulePath, modules, altClassPath, clientArgs, compatClasses, properties, servers, securityManagerConfig, classLoaderConfig, signedClientConfig, runtimeInClientConfig, version, assetIndex, dir, assetDir, recommendJavaVersion, minJavaVersion, maxJavaVersion, warnMissJavaVersion, settings, sortIndex, uuid, title, info, updateFastCheck, mainClass);
    }
}