package pro.gravit.launcher.base.profiles;

import pro.gravit.launcher.base.profiles.optional.OptionalFile;
import pro.gravit.utils.launch.LaunchOptions;

import java.util.*;

public class ClientProfileBuilder {
    private String title;
    private UUID uuid;
    private ClientProfile.Version version;
    private String info;
    private String dir;
    private int sortIndex;
    private String assetIndex;
    private String assetDir;
    private List<String> update;
    private List<String> updateExclusions;
    private List<String> updateVerify;
    private Set<OptionalFile> updateOptional;
    private List<String> jvmArgs;
    private List<String> classPath;
    private List<String> altClassPath;
    private List<String> clientArgs;
    private List<String> compatClasses;
    private List<String> loadNatives;
    private Map<String, String> properties;
    private List<ClientProfile.ServerProfile> servers;
    private ClientProfile.ClassLoaderConfig classLoaderConfig;
    private List<ClientProfile.CompatibilityFlags> flags;
    private int recommendJavaVersion;
    private int minJavaVersion;
    private int maxJavaVersion;
    private ClientProfile.ProfileDefaultSettings settings;
    private boolean limited;
    private String mainClass;
    private String mainModule;
    private LaunchOptions.ModuleConf moduleConf;

    public ClientProfileBuilder() {
        this.update = new ArrayList<>();
        this.updateExclusions = new ArrayList<>();
        this.updateVerify = new ArrayList<>();
        this.updateOptional = new HashSet<>();
        this.jvmArgs = new ArrayList<>();
        this.classPath = new ArrayList<>();
        this.altClassPath = new ArrayList<>();
        this.clientArgs = new ArrayList<>();
        this.compatClasses = new ArrayList<>();
        this.loadNatives = new ArrayList<>();
        this.properties = new HashMap<>();
        this.servers = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.settings = new ClientProfile.ProfileDefaultSettings();
        this.recommendJavaVersion = 21;
        this.minJavaVersion = 17;
        this.maxJavaVersion = 999;
        this.classLoaderConfig = ClientProfile.ClassLoaderConfig.LAUNCHER;
    }

    public ClientProfileBuilder(ClientProfile profile) {
        this.title = profile.getTitle();
        this.uuid = profile.getUUID();
        this.version = profile.getVersion();
        this.info = profile.getInfo();
        this.dir = profile.getDir();
        this.sortIndex = profile.getSortIndex();
        this.assetIndex = profile.getAssetIndex();
        this.assetDir = profile.getAssetDir();
        this.update = new ArrayList<>(profile.getUpdate());
        this.updateExclusions = new ArrayList<>(profile.getUpdateExclusions());
        this.updateVerify = new ArrayList<>(profile.getUpdateVerify());
        this.updateOptional = new HashSet<>(profile.getOptional());
        this.jvmArgs = new ArrayList<>(profile.getJvmArgs());
        this.classPath = new ArrayList<>(profile.getClassPath());
        this.altClassPath = new ArrayList<>(profile.getAlternativeClassPath());
        this.clientArgs = new ArrayList<>(profile.getClientArgs());
        this.compatClasses = new ArrayList<>(profile.getCompatClasses());
        this.loadNatives = new ArrayList<>(profile.getLoadNatives());
        this.properties = new HashMap<>(profile.getProperties());
        this.servers = new ArrayList<>(profile.getServers());
        this.classLoaderConfig = profile.getClassLoaderConfig();
        this.flags = new ArrayList<>(profile.getFlags());
        this.recommendJavaVersion = profile.getRecommendJavaVersion();
        this.minJavaVersion = profile.getMinJavaVersion();
        this.maxJavaVersion = profile.getMaxJavaVersion();
        this.settings = profile.getSettings();
        this.limited = profile.isLimited();
        this.mainClass = profile.getMainClass();
        this.mainModule = profile.getMainModule();
        this.moduleConf = profile.getModuleConf();
    }

    public ClientProfileBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public ClientProfileBuilder setUuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public ClientProfileBuilder setVersion(ClientProfile.Version version) {
        this.version = version;
        return this;
    }

    public ClientProfileBuilder setInfo(String info) {
        this.info = info;
        return this;
    }

    public ClientProfileBuilder setDir(String dir) {
        this.dir = dir;
        return this;
    }

    public ClientProfileBuilder setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
        return this;
    }

    public ClientProfileBuilder setAssetIndex(String assetIndex) {
        this.assetIndex = assetIndex;
        return this;
    }

    public ClientProfileBuilder setAssetDir(String assetDir) {
        this.assetDir = assetDir;
        return this;
    }

    public ClientProfileBuilder setUpdate(List<String> update) {
        this.update = update;
        return this;
    }

    public ClientProfileBuilder update(String value) {
        this.update.add(value);
        return this;
    }

    public ClientProfileBuilder setUpdateExclusions(List<String> updateExclusions) {
        this.updateExclusions = updateExclusions;
        return this;
    }

    public ClientProfileBuilder updateExclusions(String value) {
        this.updateExclusions.add(value);
        return this;
    }

    public ClientProfileBuilder setUpdateVerify(List<String> updateVerify) {
        this.updateVerify = updateVerify;
        return this;
    }

    public ClientProfileBuilder updateVerify(String value) {
        this.updateVerify.add(value);
        return this;
    }

    public ClientProfileBuilder setUpdateOptional(Set<OptionalFile> updateOptional) {
        this.updateOptional = updateOptional;
        return this;
    }

    public ClientProfileBuilder updateOptional(OptionalFile value) {
        this.updateOptional.add(value);
        return this;
    }

    public ClientProfileBuilder setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
        return this;
    }

    public ClientProfileBuilder jvmArg(String value) {
        this.jvmArgs.add(value);
        return this;
    }


    public ClientProfileBuilder setClassPath(List<String> classPath) {
        this.classPath = classPath;
        return this;
    }

    public ClientProfileBuilder classPath(String value) {
        this.classPath.add(value);
        return this;
    }

    public ClientProfileBuilder setAltClassPath(List<String> altClassPath) {
        this.altClassPath = altClassPath;
        return this;
    }

    public ClientProfileBuilder altClassPath(String value) {
        this.altClassPath.add(value);
        return this;
    }

    public ClientProfileBuilder setClientArgs(List<String> clientArgs) {
        this.clientArgs = clientArgs;
        return this;
    }

    public ClientProfileBuilder clientArg(String value) {
        this.clientArgs.add(value);
        return this;
    }

    public ClientProfileBuilder setCompatClasses(List<String> compatClasses) {
        this.compatClasses = compatClasses;
        return this;
    }

    public ClientProfileBuilder compatClass(String value) {
        this.compatClasses.add(value);
        return this;
    }

    public ClientProfileBuilder setLoadNatives(List<String> loadNatives) {
        this.loadNatives = loadNatives;
        return this;
    }

    public ClientProfileBuilder loadNatives(String value) {
        this.loadNatives.add(value);
        return this;
    }

    public ClientProfileBuilder setProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public ClientProfileBuilder property(String name, String value) {
        this.properties.put(name, value);
        return this;
    }

    public ClientProfileBuilder setServers(List<ClientProfile.ServerProfile> servers) {
        this.servers = servers;
        return this;
    }

    public ClientProfileBuilder server(ClientProfile.ServerProfile value) {
        this.servers.add(value);
        return this;
    }

    public ClientProfileBuilder setClassLoaderConfig(ClientProfile.ClassLoaderConfig classLoaderConfig) {
        this.classLoaderConfig = classLoaderConfig;
        return this;
    }

    public ClientProfileBuilder setFlags(List<ClientProfile.CompatibilityFlags> flags) {
        this.flags = flags;
        return this;
    }

    public ClientProfileBuilder flag(ClientProfile.CompatibilityFlags value) {
        this.flags.add(value);
        return this;
    }

    public ClientProfileBuilder setRecommendJavaVersion(int recommendJavaVersion) {
        this.recommendJavaVersion = recommendJavaVersion;
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

    public ClientProfileBuilder setSettings(ClientProfile.ProfileDefaultSettings settings) {
        this.settings = settings;
        return this;
    }

    public ClientProfileBuilder setLimited(boolean limited) {
        this.limited = limited;
        return this;
    }

    public ClientProfileBuilder setMainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public ClientProfileBuilder setMainModule(String mainModule) {
        this.mainModule = mainModule;
        return this;
    }

    public ClientProfileBuilder setModuleConf(LaunchOptions.ModuleConf moduleConf) {
        this.moduleConf = moduleConf;
        return this;
    }

    public ClientProfile createClientProfile() {
        return new ClientProfile(title, uuid, version, info, dir, sortIndex, assetIndex, assetDir, update, updateExclusions, updateVerify, updateOptional, jvmArgs, classPath, altClassPath, clientArgs, compatClasses, loadNatives, properties, servers, classLoaderConfig, flags, recommendJavaVersion, minJavaVersion, maxJavaVersion, settings, limited, mainClass, mainModule, moduleConf);
    }
}