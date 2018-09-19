package ru.gravit.launcher.profiles;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.hasher.FileNameMatcher;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.config.ConfigObject;
import ru.gravit.launcher.serialize.stream.StreamObject;
import ru.gravit.launcher.serialize.config.entry.*;

@SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
public final class
ClientProfile extends ConfigObject implements Comparable<ClientProfile> {
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
        MC1131("1.13.1", 401);
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
    @LauncherAPI
    public static final StreamObject.Adapter<ClientProfile> RO_ADAPTER = input -> new ClientProfile(input, true);

    private static final FileNameMatcher ASSET_MATCHER = new FileNameMatcher(
            new String[0], new String[]{"indexes", "objects"}, new String[0]);
    // Version
    private final StringConfigEntry version;

    private final StringConfigEntry assetIndex;
    // Client
    private final IntegerConfigEntry sortIndex;
    private final StringConfigEntry title;
    private final StringConfigEntry serverAddress;

    private final IntegerConfigEntry serverPort;
    //  Updater and client watch service
    private final ListConfigEntry update;
    private final ListConfigEntry updateExclusions;
    private final ListConfigEntry updateVerify;
    private final BooleanConfigEntry updateFastCheck;

    private final BooleanConfigEntry useWhitelist;
    // Client launcher
    private final StringConfigEntry mainClass;
    private final ListConfigEntry jvmArgs;
    private final ListConfigEntry classPath;
    private final ListConfigEntry clientArgs;

    private final ListConfigEntry whitelist;

    @LauncherAPI
    public ClientProfile(BlockConfigEntry block) {
        super(block);

        // Version
        version = block.getEntry("version", StringConfigEntry.class);
        assetIndex = block.getEntry("assetIndex", StringConfigEntry.class);

        // Client
        sortIndex = block.getEntry("sortIndex", IntegerConfigEntry.class);
        title = block.getEntry("title", StringConfigEntry.class);
        serverAddress = block.getEntry("serverAddress", StringConfigEntry.class);
        serverPort = block.getEntry("serverPort", IntegerConfigEntry.class);

        //  Updater and client watch service
        update = block.getEntry("update", ListConfigEntry.class);
        updateVerify = block.getEntry("updateVerify", ListConfigEntry.class);
        updateExclusions = block.getEntry("updateExclusions", ListConfigEntry.class);
        updateFastCheck = block.getEntry("updateFastCheck", BooleanConfigEntry.class);
        useWhitelist = block.getEntry("useWhitelist", BooleanConfigEntry.class);

        // Client launcher
        mainClass = block.getEntry("mainClass", StringConfigEntry.class);
        classPath = block.getEntry("classPath", ListConfigEntry.class);
        jvmArgs = block.getEntry("jvmArgs", ListConfigEntry.class);
        clientArgs = block.getEntry("clientArgs", ListConfigEntry.class);
        whitelist = block.getEntry("whitelist", ListConfigEntry.class);
    }

    @LauncherAPI
    public ClientProfile(HInput input, boolean ro) throws IOException {
        this(new BlockConfigEntry(input, ro));
    }

    @Override
    public int compareTo(ClientProfile o) {
        return Integer.compare(getSortIndex(), o.getSortIndex());
    }

    @LauncherAPI
    public String getAssetIndex() {
        return assetIndex.getValue();
    }

    @LauncherAPI
    public FileNameMatcher getAssetUpdateMatcher() {
        return getVersion().compareTo(Version.MC1710) >= 0 ? ASSET_MATCHER : null;
    }

    @LauncherAPI
    public String[] getClassPath() {
        return classPath.stream(StringConfigEntry.class).toArray(String[]::new);
    }

    @LauncherAPI
    public String[] getClientArgs() {
        return clientArgs.stream(StringConfigEntry.class).toArray(String[]::new);
    }

    @LauncherAPI
    public FileNameMatcher getClientUpdateMatcher() {
        String[] updateArray = update.stream(StringConfigEntry.class).toArray(String[]::new);
        String[] verifyArray = updateVerify.stream(StringConfigEntry.class).toArray(String[]::new);
        String[] exclusionsArray = updateExclusions.stream(StringConfigEntry.class).toArray(String[]::new);
        return new FileNameMatcher(updateArray, verifyArray, exclusionsArray);
    }

    @LauncherAPI
    public String[] getJvmArgs() {
        return jvmArgs.stream(StringConfigEntry.class).toArray(String[]::new);
    }
    @LauncherAPI
    public String getMainClass() {
        return mainClass.getValue();
    }

    @LauncherAPI
    public String getServerAddress() {
        return serverAddress.getValue();
    }

    @LauncherAPI
    public int getServerPort() {
        return serverPort.getValue();
    }

    @LauncherAPI
    public InetSocketAddress getServerSocketAddress() {
        return InetSocketAddress.createUnresolved(getServerAddress(), getServerPort());
    }

    @LauncherAPI
    public int getSortIndex() {
        return sortIndex.getValue();
    }

    @LauncherAPI
    public String getTitle() {
        return title.getValue();
    }

    @LauncherAPI
    public Version getVersion() {
        return Version.byName(version.getValue());
    }

    @LauncherAPI
    public boolean isUpdateFastCheck() {
        return updateFastCheck.getValue();
    }

    @LauncherAPI
    public boolean isWhitelistContains(String username)
    {
        if(!useWhitelist.getValue()) return true;
		return whitelist.stream(StringConfigEntry.class).anyMatch(e -> e.equals(username));
    }

    @LauncherAPI
    public void setTitle(String title) {
        this.title.setValue(title);
    }

    @LauncherAPI
    public void setVersion(Version version) {
        this.version.setValue(version.name);
    }

    @Override
    public String toString() {
        return title.getValue();
    }

    @LauncherAPI
    public void verify() {
        // Version
        getVersion();
        IOHelper.verifyFileName(getAssetIndex());

        // Client
        VerifyHelper.verify(getTitle(), VerifyHelper.NOT_EMPTY, "Profile title can't be empty");
        VerifyHelper.verify(getServerAddress(), VerifyHelper.NOT_EMPTY, "Server address can't be empty");
        VerifyHelper.verifyInt(getServerPort(), VerifyHelper.range(0, 65535), "Illegal server port: " + getServerPort());

        //  Updater and client watch service
        update.verifyOfType(ConfigEntry.Type.STRING);
        updateVerify.verifyOfType(ConfigEntry.Type.STRING);
        updateExclusions.verifyOfType(ConfigEntry.Type.STRING);

        // Client launcher
        jvmArgs.verifyOfType(ConfigEntry.Type.STRING);
        classPath.verifyOfType(ConfigEntry.Type.STRING);
        clientArgs.verifyOfType(ConfigEntry.Type.STRING);
        VerifyHelper.verify(getTitle(), VerifyHelper.NOT_EMPTY, "Main class can't be empty");
    }
}
