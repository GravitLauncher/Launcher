package pro.gravit.launcher.profiles;

import pro.gravit.launcher.LauncherAPI;
import pro.gravit.launcher.hasher.FileNameMatcher;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.profiles.optional.OptionalDepend;
import pro.gravit.launcher.profiles.optional.OptionalFile;
import pro.gravit.launcher.profiles.optional.OptionalType;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

public final class ClientProfile implements Comparable<ClientProfile> {
    @LauncherAPI
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
        MC1144("1.14.4", 498);
        private static final Map<String, Version> VERSIONS;

        static {
            Version[] versionsValues = values();
            VERSIONS = new HashMap<>(versionsValues.length);
            for (Version version : versionsValues)
                VERSIONS.put(version.name, version);
        }

        public static Version byName(String name) {
            return VerifyHelper.getMapValue(VERSIONS, name, String.format("Unknown client version: '%s'", name));
        }

        public final String name;

        public final int protocol;

        Version(String name, int protocol) {
            this.name = name;
            this.protocol = protocol;
        }

        @Override
        public String toString() {
            return "Minecraft " + name;
        }
    }

    public static final boolean profileCaseSensitive = Boolean.getBoolean("launcher.clientProfile.caseSensitive");

    private static final FileNameMatcher ASSET_MATCHER = new FileNameMatcher(
            new String[0], new String[]{"indexes", "objects"}, new String[0]);
    // Version
    @LauncherAPI
    private String version;
    @LauncherAPI
    private String assetIndex;

    @LauncherAPI
    private String dir;
    @LauncherAPI
    private String assetDir;
    // Client
    @LauncherAPI
    private int sortIndex;
    @LauncherAPI
    private String title;
    @LauncherAPI
    private String info;
    @LauncherAPI
    private String serverAddress;
    @LauncherAPI
    private int serverPort;

    //  Updater and client watch service
    @LauncherAPI
    private final List<String> update = new ArrayList<>();
    @LauncherAPI
    private final List<String> updateExclusions = new ArrayList<>();
    @LauncherAPI
    private final List<String> updateShared = new ArrayList<>();
    @LauncherAPI
    private final List<String> updateVerify = new ArrayList<>();
    @LauncherAPI
    private final Set<OptionalFile> updateOptional = new HashSet<>();
    @LauncherAPI
    private boolean updateFastCheck;
    @LauncherAPI
    private boolean useWhitelist;
    // Client launcher
    @LauncherAPI
    private String mainClass;
    @LauncherAPI
    private final List<String> jvmArgs = new ArrayList<>();
    @LauncherAPI
    private final List<String> classPath = new ArrayList<>();
    @LauncherAPI
    private final List<String> clientArgs = new ArrayList<>();
    @LauncherAPI
    private final List<String> whitelist = new ArrayList<>();

    @Override
    public int compareTo(ClientProfile o) {
        return Integer.compare(getSortIndex(), o.getSortIndex());
    }

    @LauncherAPI
    public String getAssetIndex() {
        return assetIndex;
    }

    @LauncherAPI
    public FileNameMatcher getAssetUpdateMatcher() {
        return getVersion().compareTo(Version.MC1710) >= 0 ? ASSET_MATCHER : null;
    }

    @LauncherAPI
    public String[] getClassPath() {
        return classPath.toArray(new String[0]);
    }

    @LauncherAPI
    public String[] getClientArgs() {
        return clientArgs.toArray(new String[0]);
    }

    @LauncherAPI
    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    @LauncherAPI
    public String getAssetDir() {
        return assetDir;
    }

    @LauncherAPI
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

    @LauncherAPI
    public String[] getJvmArgs() {
        return jvmArgs.toArray(new String[0]);
    }

    @LauncherAPI
    public String getMainClass() {
        return mainClass;
    }

    @LauncherAPI
    public String getServerAddress() {
        return serverAddress;
    }

    @LauncherAPI
    public Set<OptionalFile> getOptional() {
        return updateOptional;
    }

    @LauncherAPI
    public void updateOptionalGraph() {
        for (OptionalFile file : updateOptional) {
            if (file.dependenciesFile != null) {
                file.dependencies = new OptionalFile[file.dependenciesFile.length];
                for (int i = 0; i < file.dependenciesFile.length; ++i) {
                    file.dependencies[i] = getOptionalFile(file.dependenciesFile[i].name, file.dependenciesFile[i].type);
                }
            }
            if (file.conflictFile != null) {
                file.conflict = new OptionalFile[file.conflictFile.length];
                for (int i = 0; i < file.conflictFile.length; ++i) {
                    file.conflict[i] = getOptionalFile(file.conflictFile[i].name, file.conflictFile[i].type);
                }
            }
        }
    }

    @LauncherAPI
    public OptionalFile getOptionalFile(String file, OptionalType type) {
        for (OptionalFile f : updateOptional)
            if (f.type.equals(type) && f.name.equals(file)) return f;
        return null;
    }

    @LauncherAPI
    public Collection<String> getShared() {
        return updateShared;
    }

    @LauncherAPI
    public void markOptional(String name, OptionalType type) {
        OptionalFile file = getOptionalFile(name, type);
        if (file == null) {
            throw new SecurityException(String.format("Optional %s not found in optionalList", name));
        }
        markOptional(file);
    }

    @LauncherAPI
    public void markOptional(OptionalFile file) {

        if (file.mark) return;
        file.mark = true;
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

    @LauncherAPI
    public void unmarkOptional(String name, OptionalType type) {
        OptionalFile file = getOptionalFile(name, type);
        if (file == null) {
            throw new SecurityException(String.format("Optional %s not found in optionalList", name));
        }
        unmarkOptional(file);
    }

    @LauncherAPI
    public void unmarkOptional(OptionalFile file) {
        if (!file.mark) return;
        file.mark = false;
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

    public void pushOptionalFile(HashedDir dir, boolean digest) {
        for (OptionalFile opt : updateOptional) {
            if (opt.type.equals(OptionalType.FILE) && !opt.mark) {
                for (String file : opt.list)
                    dir.removeR(file);
            }
        }
    }

    public void pushOptionalJvmArgs(Collection<String> jvmArgs1) {
        for (OptionalFile opt : updateOptional) {
            if (opt.type.equals(OptionalType.JVMARGS) && opt.mark) {
                jvmArgs1.addAll(Arrays.asList(opt.list));
            }
        }
    }

    public void pushOptionalClientArgs(Collection<String> clientArgs1) {
        for (OptionalFile opt : updateOptional) {
            if (opt.type.equals(OptionalType.CLIENTARGS) && opt.mark) {
                clientArgs1.addAll(Arrays.asList(opt.list));
            }
        }
    }

    public void pushOptionalClassPath(pushOptionalClassPathCallback callback) throws IOException {
        for (OptionalFile opt : updateOptional) {
            if (opt.type.equals(OptionalType.CLASSPATH) && opt.mark) {
                callback.run(opt.list);
            }
        }
    }

    @FunctionalInterface
    public interface pushOptionalClassPathCallback {
        void run(String[] opt) throws IOException;
    }

    @LauncherAPI
    public int getServerPort() {
        return serverPort;
    }

    @LauncherAPI
    public InetSocketAddress getServerSocketAddress() {
        return InetSocketAddress.createUnresolved(getServerAddress(), getServerPort());
    }

    @LauncherAPI
    public int getSortIndex() {
        return sortIndex;
    }

    @LauncherAPI
    public String getTitle() {
        return title;
    }

    @LauncherAPI
    public String getInfo() {
        return info;
    }

    @LauncherAPI
    public Version getVersion() {
        return Version.byName(version);
    }

    @LauncherAPI
    public boolean isUpdateFastCheck() {
        return updateFastCheck;
    }

    @LauncherAPI
    public boolean isWhitelistContains(String username) {
        if (!useWhitelist) return true;
        return whitelist.stream().anyMatch(profileCaseSensitive ? e -> e.equals(username) : e -> e.equalsIgnoreCase(username));
    }

    @LauncherAPI
    public void setTitle(String title) {
        this.title = title;
    }

    @LauncherAPI
    public void setInfo(String info) {
        this.info = info;
    }

    @LauncherAPI
    public void setVersion(Version version) {
        this.version = version.name;
    }

    @Override
    public String toString() {
        return title;
    }

    @LauncherAPI
    public void verify() {
        // Version
        getVersion();
        IOHelper.verifyFileName(getAssetIndex());

        // Client
        VerifyHelper.verify(getTitle(), VerifyHelper.NOT_EMPTY, "Profile title can't be empty");
        VerifyHelper.verify(getInfo(), VerifyHelper.NOT_EMPTY, "Profile info can't be empty");
        VerifyHelper.verify(getServerAddress(), VerifyHelper.NOT_EMPTY, "Server address can't be empty");
        VerifyHelper.verifyInt(getServerPort(), VerifyHelper.range(0, 65535), "Illegal server port: " + getServerPort());

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
        for (OptionalFile f : updateOptional) {
            if (f == null) throw new IllegalArgumentException("Found null entry in updateOptional");
            if (f.name == null) throw new IllegalArgumentException("Optional: name must not be null");
            if (f.list == null) throw new IllegalArgumentException("Optional: list must not be null");
            for (String s : f.list) {
                if (s == null)
                    throw new IllegalArgumentException(String.format("Found null entry in updateOptional.%s.list", f.name));
            }
            if (f.conflictFile != null) for (OptionalDepend s : f.conflictFile) {
                if (s == null)
                    throw new IllegalArgumentException(String.format("Found null entry in updateOptional.%s.conflictFile", f.name));
            }
            if (f.dependenciesFile != null) for (OptionalDepend s : f.dependenciesFile) {
                if (s == null)
                    throw new IllegalArgumentException(String.format("Found null entry in updateOptional.%s.dependenciesFile", f.name));
            }
        }
    }


}
