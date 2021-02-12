package pro.gravit.launcher.profiles;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.hasher.FileNameMatcher;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.profiles.optional.OptionalDepend;
import pro.gravit.launcher.profiles.optional.OptionalFile;
import pro.gravit.launcher.profiles.optional.OptionalTrigger;
import pro.gravit.launcher.profiles.optional.OptionalType;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

public final class ClientProfile implements Comparable<ClientProfile> {

    public static final boolean profileCaseSensitive = Boolean.getBoolean("launcher.clientProfile.caseSensitive");
    private static final FileNameMatcher ASSET_MATCHER = new FileNameMatcher(
            new String[0], new String[]{"indexes", "objects"}, new String[0]);
    //  Updater and client watch service
    @LauncherNetworkAPI
    private final List<String> update = new ArrayList<>();
    @LauncherNetworkAPI
    private final List<String> updateExclusions = new ArrayList<>();
    @LauncherNetworkAPI
    private final List<String> updateShared = new ArrayList<>();
    @LauncherNetworkAPI
    private final List<String> updateVerify = new ArrayList<>();
    @LauncherNetworkAPI
    private final Set<OptionalFile> updateOptional = new HashSet<>();
    @LauncherNetworkAPI
    private final List<String> jvmArgs = new ArrayList<>();
    @LauncherNetworkAPI
    private final List<String> classPath = new ArrayList<>();
    @LauncherNetworkAPI
    private final List<String> altClassPath = new ArrayList<>();
    @LauncherNetworkAPI
    private final List<String> clientArgs = new ArrayList<>();
    @LauncherNetworkAPI
    public SecurityManagerConfig securityManagerConfig = SecurityManagerConfig.CLIENT;
    @LauncherNetworkAPI
    public ClassLoaderConfig classLoaderConfig = ClassLoaderConfig.LAUNCHER;
    @LauncherNetworkAPI
    public SignedClientConfig signedClientConfig = SignedClientConfig.NONE;
    // Version
    @LauncherNetworkAPI
    private String version;
    @LauncherNetworkAPI
    private String assetIndex;
    @LauncherNetworkAPI
    private String dir;
    @LauncherNetworkAPI
    private String assetDir;
    @LauncherNetworkAPI
    private int recommendJavaVersion = 8;
    @LauncherNetworkAPI
    private int minJavaVersion = 8;
    @LauncherNetworkAPI
    private int maxJavaVersion = 17;
    @LauncherNetworkAPI
    private boolean warnMissJavaVersion = true;
    // Client
    @LauncherNetworkAPI
    private int sortIndex;
    @LauncherNetworkAPI
    private UUID uuid;
    @LauncherNetworkAPI
    private String title;
    @LauncherNetworkAPI
    private String info;
    @Deprecated
    @LauncherNetworkAPI
    private String serverAddress;
    @Deprecated
    @LauncherNetworkAPI
    private int serverPort;
    @LauncherNetworkAPI
    private boolean updateFastCheck;
    // Client launcher
    @LauncherNetworkAPI
    private String mainClass;
    @LauncherNetworkAPI
    private final List<String> compatClasses = new ArrayList<>();
    @LauncherNetworkAPI
    private final Map<String, String> properties = new HashMap<>();

    public static class ServerProfile {
        public String name;
        public String serverAddress;
        public int serverPort;
        public boolean isDefault = true;

        public InetSocketAddress toSocketAddress() {
            return InetSocketAddress.createUnresolved(serverAddress, serverPort);
        }
    }

    @LauncherNetworkAPI
    private List<ServerProfile> servers = new ArrayList<>(1);

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

    public void setRecommendJavaVersion(int recommendJavaVersion) {
        this.recommendJavaVersion = recommendJavaVersion;
    }

    public int getMinJavaVersion() {
        return minJavaVersion;
    }

    public void setMinJavaVersion(int minJavaVersion) {
        this.minJavaVersion = minJavaVersion;
    }

    public int getMaxJavaVersion() {
        return maxJavaVersion;
    }

    public void setMaxJavaVersion(int maxJavaVersion) {
        this.maxJavaVersion = maxJavaVersion;
    }

    public boolean isWarnMissJavaVersion() {
        return warnMissJavaVersion;
    }

    public void setWarnMissJavaVersion(boolean warnMissJavaVersion) {
        this.warnMissJavaVersion = warnMissJavaVersion;
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

    @Deprecated
    public OptionalFile getOptionalFile(String file, OptionalType type) {
        for (OptionalFile f : updateOptional)
            if (f.type.equals(type) && f.name.equals(file)) return f;
        return null;
    }

    public OptionalFile getOptionalFile(String file) {
        for (OptionalFile f : updateOptional)
            if (f.name.equals(file)) return f;
        return null;
    }

    public Collection<String> getShared() {
        return updateShared;
    }

    @Deprecated
    public void markOptional(OptionalFile file) {

        if (file.mark) return;
        file.mark = true;
        file.watchEvent(true);
        if (file.dependencies != null) {
            for (OptionalFile dep : file.dependencies) {
                if (dep.dependenciesCount == null) dep.dependenciesCount = new HashSet<>();
                dep.dependenciesCount.add(file);
                markOptional(dep);
            }
        }
        if (file.conflict != null) {
            for (OptionalFile conflict : file.conflict) {
                unmarkOptional(conflict);
            }
        }
    }

    @Deprecated
    public void unmarkOptional(OptionalFile file) {
        if (!file.mark) return;
        file.mark = false;
        file.watchEvent(false);
        if (file.dependenciesCount != null) {
            for (OptionalFile f : file.dependenciesCount) {
                if (f.isPreset) continue;
                unmarkOptional(f);
            }
            file.dependenciesCount.clear();
            file.dependenciesCount = null;
        }
        if (file.dependencies != null) {
            for (OptionalFile f : file.dependencies) {
                if (!f.mark) continue;
                if (f.dependenciesCount == null) {
                    unmarkOptional(f);
                } else if (f.dependenciesCount.size() <= 1) {
                    f.dependenciesCount.clear();
                    f.dependenciesCount = null;
                    unmarkOptional(f);
                }
            }
        }
    }

    @Deprecated
    public void pushOptionalFile(HashedDir dir, boolean digest) {
        for (OptionalFile opt : updateOptional) {
            if (opt.type.equals(OptionalType.FILE) && !opt.mark) {
                for (String file : opt.list)
                    dir.removeR(file);
            }
        }
    }

    @Deprecated
    public void pushOptionalJvmArgs(Collection<String> jvmArgs1) {
        for (OptionalFile opt : updateOptional) {
            if (opt.type.equals(OptionalType.JVMARGS) && opt.mark) {
                jvmArgs1.addAll(Arrays.asList(opt.list));
            }
        }
    }

    @Deprecated
    public void pushOptionalClientArgs(Collection<String> clientArgs1) {
        for (OptionalFile opt : updateOptional) {
            if (opt.type.equals(OptionalType.CLIENTARGS) && opt.mark) {
                clientArgs1.addAll(Arrays.asList(opt.list));
            }
        }
    }

    @Deprecated
    public void pushOptionalClassPath(pushOptionalClassPathCallback callback) throws IOException {
        for (OptionalFile opt : updateOptional) {
            if (opt.type.equals(OptionalType.CLASSPATH) && opt.mark) {
                callback.run(opt.list);
            }
        }
    }

    public int getServerPort() {
        ServerProfile profile = getDefaultServerProfile();
        return profile == null ? 25565 : profile.serverPort;
    }

    @Deprecated
    public InetSocketAddress getServerSocketAddress() {
        return InetSocketAddress.createUnresolved(getServerAddress(), getServerPort());
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
        return title;
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
        for(String s : compatClasses) {
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
            if (f.triggers != null) {
                for (OptionalTrigger trigger : f.triggers) {
                    if (trigger == null)
                        throw new IllegalArgumentException(String.format("Found null entry in updateOptional.%s.triggers", f.name));
                    if (trigger.type == null)
                        throw new IllegalArgumentException(String.format("trigger.type must not be null in updateOptional.%s.triggers", f.name));
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
        MC1165("1.16.5", 754);
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
        AGENT, LAUNCHER
    }

    public enum SignedClientConfig {
        NONE, SIGNED
    }

    @FunctionalInterface
    public interface pushOptionalClassPathCallback {
        void run(String[] opt) throws IOException;
    }


}
