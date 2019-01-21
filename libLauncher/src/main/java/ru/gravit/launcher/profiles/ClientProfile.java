package ru.gravit.launcher.profiles;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.hasher.FileNameMatcher;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.net.InetSocketAddress;
import java.util.*;

public final class ClientProfile implements Comparable<ClientProfile> {
    @LauncherAPI
    public enum Version {
        MC147("1.4.7", 51),
        MC152("1.5.2", 61),
        MC164("1.6.4", 78),
        MC172("1.7.2", 4),
        MC1710("1.7.10", 5),
        MC189("1.8.9", 47),
        MC194("1.9.4", 110),
        MC1102("1.10.2", 210),
        MC1112("1.11.2", 316),
        MC1122("1.12.2", 340),
        MC113("1.13", 393),
        MC1131("1.13.1", 401),
        MC1132("1.13.2", 402);
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

    public static class OptionalFile {
        @LauncherAPI
        public String file;
        @LauncherAPI
        public boolean mark;
        @LauncherAPI
        public String name;
        @LauncherAPI
        public String info;
        @LauncherAPI
        public String[] dependenciesFile;
        @LauncherAPI
        public String[] conflictFile;
        @LauncherAPI
        public transient OptionalFile[] dependencies;
        @LauncherAPI
        public transient OptionalFile[] conflict;
        @LauncherAPI
        public int subTreeLevel = 1;
        @LauncherAPI
        public boolean isAdminOnly = false;
        @LauncherAPI
        public transient Set<OptionalFile> dependenciesCount;

        public OptionalFile(String file, boolean mark) {
            this.file = file;
            this.mark = mark;
        }

        public OptionalFile(String file) {
            this.file = file;
            this.mark = false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OptionalFile that = (OptionalFile) o;
            return Objects.equals(file, that.file);
        }

        @Override
        public int hashCode() {
            return Objects.hash(file);
        }
    }

    public static class OptionalArgs {
        @LauncherAPI
        public boolean mark;
        @LauncherAPI
        public String[] args;

        public OptionalArgs(String[] args, boolean mark) {
            this.mark = mark;
            this.args = args;
        }
    }

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
    @LauncherAPI
    private final List<OptionalArgs> optionalJVMArgs = new ArrayList<>();
    @LauncherAPI
    private final List<OptionalArgs> optionalClientArgs = new ArrayList<>();
    @LauncherAPI
    private final List<OptionalArgs> optionalClassPath = new ArrayList<>();

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
    public List<OptionalArgs> getOptionalJVMArgs() {
        return optionalJVMArgs;
    }

    @LauncherAPI
    public List<OptionalArgs> getOptionalClientArgs() {
        return optionalClientArgs;
    }

    @LauncherAPI
    public List<OptionalArgs> getOptionalClassPath() {
        return optionalClassPath;
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
                    file.dependencies[i] = getOptionalFile(file.dependenciesFile[i]);
                }
            }
            if (file.conflictFile != null) {
                file.conflict = new OptionalFile[file.conflictFile.length];
                for (int i = 0; i < file.conflictFile.length; ++i) {
                    file.conflict[i] = getOptionalFile(file.conflictFile[i]);
                }
            }
        }
    }

    @LauncherAPI
    public OptionalFile getOptionalFile(String file) {
        for (OptionalFile f : updateOptional)
            if (f.file.equals(file)) return f;
        return null;
    }

    @LauncherAPI
    public Collection<String> getShared() {
        return updateShared;
    }

    @LauncherAPI
    public void markOptional(String opt) {
        if (!updateOptional.contains(new OptionalFile(opt)))
            throw new SecurityException(String.format("Optional mod %s not found in optionalList", opt));
        OptionalFile file = getOptionalFile(opt);
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
    public void unmarkOptional(String opt) {
        if (!updateOptional.contains(new OptionalFile(opt)))
            throw new SecurityException(String.format("Optional mod %s not found in optionalList", opt));
        OptionalFile file = getOptionalFile(opt);
        unmarkOptional(file);
    }

    @LauncherAPI
    public void unmarkOptional(OptionalFile file) {
        if (!file.mark) return;
        file.mark = false;
        if (file.dependenciesCount != null) {
            for (OptionalFile f : file.dependenciesCount) {
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

    public void pushOptional(HashedDir dir, boolean digest) {
        for (OptionalFile opt : updateOptional) {
            if (!opt.mark) dir.removeR(opt.file);
        }
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
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((assetDir == null) ? 0 : assetDir.hashCode());
        result = prime * result + ((assetIndex == null) ? 0 : assetIndex.hashCode());
        result = prime * result + ((classPath == null) ? 0 : classPath.hashCode());
        result = prime * result + ((clientArgs == null) ? 0 : clientArgs.hashCode());
        result = prime * result + ((dir == null) ? 0 : dir.hashCode());
        result = prime * result + ((jvmArgs == null) ? 0 : jvmArgs.hashCode());
        result = prime * result + ((mainClass == null) ? 0 : mainClass.hashCode());
        result = prime * result + ((serverAddress == null) ? 0 : serverAddress.hashCode());
        result = prime * result + serverPort;
        result = prime * result + sortIndex;
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result + ((info == null) ? 0 : info.hashCode());
        result = prime * result + ((update == null) ? 0 : update.hashCode());
        result = prime * result + ((updateExclusions == null) ? 0 : updateExclusions.hashCode());
        result = prime * result + (updateFastCheck ? 1231 : 1237);
        result = prime * result + ((updateOptional == null) ? 0 : updateOptional.hashCode());
        result = prime * result + ((updateShared == null) ? 0 : updateShared.hashCode());
        result = prime * result + ((updateVerify == null) ? 0 : updateVerify.hashCode());
        result = prime * result + (useWhitelist ? 1231 : 1237);
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        result = prime * result + ((whitelist == null) ? 0 : whitelist.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ClientProfile other = (ClientProfile) obj;
        if (assetDir == null) {
            if (other.assetDir != null)
                return false;
        } else if (!assetDir.equals(other.assetDir))
            return false;
        if (assetIndex == null) {
            if (other.assetIndex != null)
                return false;
        } else if (!assetIndex.equals(other.assetIndex))
            return false;
        if (classPath == null) {
            if (other.classPath != null)
                return false;
        } else if (!classPath.equals(other.classPath))
            return false;
        if (clientArgs == null) {
            if (other.clientArgs != null)
                return false;
        } else if (!clientArgs.equals(other.clientArgs))
            return false;
        if (dir == null) {
            if (other.dir != null)
                return false;
        } else if (!dir.equals(other.dir))
            return false;
        if (jvmArgs == null) {
            if (other.jvmArgs != null)
                return false;
        } else if (!jvmArgs.equals(other.jvmArgs))
            return false;
        if (mainClass == null) {
            if (other.mainClass != null)
                return false;
        } else if (!mainClass.equals(other.mainClass))
            return false;
        if (serverAddress == null) {
            if (other.serverAddress != null)
                return false;
        } else if (!serverAddress.equals(other.serverAddress))
            return false;
        if (serverPort != other.serverPort)
            return false;
        if (sortIndex != other.sortIndex)
            return false;
        if (title == null) {
            if (other.title != null)
                return false;
        } else if (!title.equals(other.title))
            return false;
        if (info == null) {
            if (other.info != null)
                return false;
        } else if (!info.equals(other.info))
            return false;
        if (update == null) {
            if (other.update != null)
                return false;
        } else if (!update.equals(other.update))
            return false;
        if (updateExclusions == null) {
            if (other.updateExclusions != null)
                return false;
        } else if (!updateExclusions.equals(other.updateExclusions))
            return false;
        if (updateFastCheck != other.updateFastCheck)
            return false;
        if (updateOptional == null) {
            if (other.updateOptional != null)
                return false;
        } else if (!updateOptional.equals(other.updateOptional))
            return false;
        if (updateShared == null) {
            if (other.updateShared != null)
                return false;
        } else if (!updateShared.equals(other.updateShared))
            return false;
        if (updateVerify == null) {
            if (other.updateVerify != null)
                return false;
        } else if (!updateVerify.equals(other.updateVerify))
            return false;
        if (useWhitelist != other.useWhitelist)
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        if (whitelist == null) {
            return other.whitelist == null;
        } else return whitelist.equals(other.whitelist);
    }
}
