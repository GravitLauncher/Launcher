package pro.gravit.launchserver.auth.profiles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launcher.core.serialize.HInput;
import pro.gravit.launcher.core.serialize.HOutput;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.launchserver.modules.events.LaunchServerUpdatesSyncEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.IOHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class LocalProfilesProvider extends ProfilesProvider implements Reconfigurable {
    private final transient Logger logger = LogManager.getLogger();
    public String profilesDir = "profiles";
    public String cacheFile = ".updates-cache";
    public String updatesDir = "updates";
    public boolean cacheUpdates = true;
    private transient volatile Map<String, HashedDir> updatesDirMap;
    private transient volatile Map<UUID, LocalProfile> profilesMap;

    @Override
    public UncompletedProfile create(String name, String description, CompletedProfile reference) {
        LocalProfile ref = (LocalProfile) reference;
        LocalProfile profile;
        if(ref != null) {
            ClientProfile newClientProfile = new ClientProfileBuilder(ref.profile)
                    .setTitle(name)
                    .setInfo(description)
                    .setDir(name)
                    .createClientProfile();
            profile = new LocalProfile(newClientProfile, ref.clientDir, ref.assetDir);
            Path updatesDirPath = Path.of(updatesDir);
            try {
                IOHelper.copy(updatesDirPath.resolve(ref.profile.getDir()), updatesDirPath.resolve(profile.profile.getDir()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            profile = new LocalProfile(new ClientProfileBuilder()
                    .setUuid(UUID.randomUUID())
                    .setTitle(name)
                    .setInfo(description)
                    .setDir(name)
                    .setAssetDir("assets")
                    .createClientProfile(), null, getUpdatesDir("assets"));
        }
        pushProfileAndSave(profile);
        return profile;
    }

    @Override
    public void delete(UncompletedProfile profile) {
        LocalProfile p = (LocalProfile) profile;
        profilesMap.remove(p.getUuid());
        try {
            Path updatesDirPath = Path.of(updatesDir);
            IOHelper.deleteDir(updatesDirPath.resolve(p.profile.getDir()), true);
            Files.delete(p.getConfigPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<UncompletedProfile> getProfiles(Client client) {
        if(client == null) {
            return new HashSet<>(profilesMap.values());
        }
        if(!client.isAuth) {
            return new HashSet<>();
        }
        Set<UncompletedProfile> profiles = new HashSet<>();
        for(var p : profilesMap.entrySet()) {
            var uuid = p.getKey();
            var profile = p.getValue();
            if(profile.getProfile() != null && profile.getProfile().isLimited()) {
                if(client.isAuth && client.permissions != null && client.permissions.hasPerm(String.format("launchserver.profile.%s.show", uuid))) {
                    profiles.add(profile);
                }
            } else {
                profiles.add(profile);
            }
        }
        return profiles;
    }

    @Override
    public CompletedProfile pushUpdate(UncompletedProfile profile, String tag, ClientProfile clientProfile, List<ProfileAction> assetActions, List<ProfileAction> clientActions, List<UpdateFlag> flags) throws IOException {
        Path updatesDirPath = Path.of(updatesDir);
        LocalProfile localProfile = (LocalProfile) profile;
        localProfile = new LocalProfile(clientProfile, localProfile.clientDir, localProfile.assetDir);
        localProfile.profile = clientProfile;
        if(flags.contains(UpdateFlag.USE_DEFAULT_ASSETS)) {
            if(getUpdatesDir("assets") == null) {
                Path assetDirPath = updatesDirPath.resolve("assets");
                if(!Files.exists(assetDirPath)) {
                    Files.createDirectories(assetDirPath);
                }
                var assetsHDir = new HashedDir(assetDirPath, null, true, true);
                updatesDirMap.put("assets", assetsHDir);
                localProfile.assetDir = assetsHDir;
            }
        }
        if(assetActions != null && !assetActions.isEmpty()) {
            Path assetDir = updatesDirPath.resolve(clientProfile.getAssetDir());
            execute(localProfile.assetDir, assetDir, assetActions);
            localProfile.assetDir = new HashedDir(assetDir, null, true, true);
        }
        if(clientActions != null && !clientActions.isEmpty()) {
            Path clientDir = updatesDirPath.resolve(clientProfile.getDir());
            execute(localProfile.clientDir, clientDir, clientActions);
            localProfile.clientDir = new HashedDir(clientDir, null, true, true);
        }
        pushProfileAndSave(localProfile);
        return localProfile;
    }

    private void pushProfileAndSave(LocalProfile localProfile) {
        profilesMap.put(localProfile.getUuid(), localProfile);
        try(Writer writer = IOHelper.newWriter(localProfile.getConfigPath())) {
            Launcher.gsonManager.configGson.toJson(localProfile.profile, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void download(CompletedProfile profile, Map<String, Path> files, boolean assets) throws IOException {
        Path sourceDir = Path.of(updatesDir).resolve(assets ? profile.getProfile().getAssetDir() : profile.getProfile().getDir());
        for(var e : files.entrySet()) {
            var source = sourceDir.resolve(e.getKey());
            var target = e.getValue();
            IOHelper.createParentDirs(target);
            IOHelper.copy(source, target);
        }
    }

    @Override
    public HashedDir getUnconnectedDirectory(String name) {
        return getUpdatesDir(name);
    }

    @Override
    public CompletedProfile get(UUID uuid, String tag) {
        return profilesMap.get(uuid);
    }

    @Override
    public CompletedProfile get(String name, String tag) {
        for(var p : profilesMap.values()) {
            if(p.getName() != null && p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    private void writeCache(Path file) throws IOException {
        try (HOutput output = new HOutput(IOHelper.newOutput(file))) {
            output.writeLength(updatesDirMap.size(), 0);
            for (Map.Entry<String, HashedDir> entry : updatesDirMap.entrySet()) {
                output.writeString(entry.getKey(), 0);
                entry.getValue().write(output);
            }
        }
        logger.debug("Saved {} updates to cache", updatesDirMap.size());
    }

    private void readCache(Path file) throws IOException {
        Map<String, HashedDir> updatesDirMap = new ConcurrentHashMap<>(16);
        try (HInput input = new HInput(IOHelper.newInput(file))) {
            int size = input.readLength(0);
            for (int i = 0; i < size; ++i) {
                String name = input.readString(0);
                HashedDir dir = new HashedDir(input);
                updatesDirMap.put(name, dir);
            }
        }
        logger.debug("Found {} updates from cache", updatesDirMap.size());
        this.updatesDirMap = updatesDirMap;
    }

    public void readProfilesDir() throws IOException {
        Path profilesDirPath = Path.of(profilesDir);
        Map<UUID, LocalProfile> newProfiles = new ConcurrentHashMap<>();
        IOHelper.walk(profilesDirPath, new ProfilesFileVisitor(newProfiles), false);
        profilesMap = newProfiles;
    }

    public void readUpdatesDir() throws IOException {
        var cacheFilePath = Path.of(cacheFile);
        if (cacheUpdates) {
            if (Files.exists(cacheFilePath)) {
                try {
                    readCache(cacheFilePath);
                    return;
                } catch (Throwable e) {
                    logger.error("Read updates cache failed", e);
                }
            }
        }
        sync(null);
    }

    public void sync(Collection<String> dirs) throws IOException {
        logger.info("Syncing updates dir");
        Map<String, HashedDir> newUpdatesDirMap = new ConcurrentHashMap<>(16);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Path.of(updatesDir))) {
            for (final Path updateDir : dirStream) {
                if (Files.isHidden(updateDir))
                    continue; // Skip hidden

                // Resolve name and verify is dir
                String name = IOHelper.getFileName(updateDir);
                if (!IOHelper.isDir(updateDir)) {
                    if (!IOHelper.isFile(updateDir) && Stream.of(".jar", ".exe", ".hash").noneMatch(e -> updateDir.toString().endsWith(e)))
                        logger.warn("Not update dir: '{}'", name);
                    continue;
                }

                // Add from previous map (it's guaranteed to be non-null)
                if (dirs != null && !dirs.contains(name)) {
                    HashedDir hdir = updatesDirMap.get(name);
                    if (hdir != null) {
                        newUpdatesDirMap.put(name, hdir);
                        continue;
                    }
                }

                // Sync and sign update dir
                logger.info("Syncing '{}' update dir", name);
                HashedDir updateHDir = new HashedDir(updateDir, null, true, true);
                newUpdatesDirMap.put(name, updateHDir);
            }
        }
        updatesDirMap = newUpdatesDirMap;
        if (cacheUpdates) {
            try {
                writeCache(Path.of(cacheFile));
            } catch (Throwable e) {
                logger.error("Write updates cache failed", e);
            }
        }
        server.modulesManager.invokeEvent(new LaunchServerUpdatesSyncEvent(server));
    }

    public HashedDir getUpdatesDir(String updateName) {
        if(updateName == null) {
            return null;
        }
        return updatesDirMap.get(updateName);
    }

    @Override
    public void init(LaunchServer server) {
        super.init(server);
        if(server.env == LaunchServer.LaunchServerEnv.TEST) {
            return;
        }
        try {
            if (!IOHelper.isDir(Path.of(updatesDir)))
                Files.createDirectory(Path.of(updatesDir));
            readUpdatesDir();
        } catch (IOException e) {
            logger.error("Updates not synced", e);
        }
        try {
            Path profilesDirPath = Path.of(profilesDir);
            if (!IOHelper.isDir(profilesDirPath))
                Files.createDirectory(profilesDirPath);
            readProfilesDir();
        } catch (IOException e) {
            logger.error("Profiles not synced", e);
        }
    }

    public static void execute(HashedDir dir, Path updatesDirPath, List<ProfileAction> actions) throws IOException {
        for(var action : actions) {
            execute(dir, updatesDirPath, action);
        }
    }

    public static void execute(HashedDir dir, Path updatesDirPath, ProfileAction action) throws IOException {
        switch (action.type()) {
            case UPLOAD -> {
                Path target = updatesDirPath.resolve(action.target());
                if(action.source() == null) {
                    IOHelper.createParentDirs(target);
                    IOHelper.transfer(action.input().get(), target);
                } else {
                    Path source = Path.of(action.source());
                    if(source.toAbsolutePath().equals(target.toAbsolutePath())) {
                        return;
                    }
                    if(action.deleteSource()) {
                        IOHelper.createParentDirs(target);
                        IOHelper.move(source, target);
                    } else {
                        IOHelper.createParentDirs(target);
                        IOHelper.copy(source, target);
                    }
                }
            }
            case COPY -> {
                Path source = updatesDirPath.resolve(action.source());
                Path target = updatesDirPath.resolve(action.target());
                if(source.toAbsolutePath().equals(target.toAbsolutePath())) {
                    return;
                }
                IOHelper.createParentDirs(target);
                IOHelper.copy(source, target);
            }
            case MOVE -> {
                Path source = updatesDirPath.resolve(action.source());
                Path target = updatesDirPath.resolve(action.target());
                if(source.toAbsolutePath().equals(target.toAbsolutePath())) {
                    return;
                }
                IOHelper.createParentDirs(target);
                IOHelper.move(source, target);
            }
            case DELETE -> {
                Path target = updatesDirPath.resolve(action.target());
                if(Files.isDirectory(target)) {
                    IOHelper.deleteDir(target, true);
                }
            }
        }
    }

    @Override
    public Map<String, Command> getCommands() {
        return Map.of( "sync",
                new SubCommand("[]", "sync all") {
                    @Override
                    public void invoke(String... args) throws Exception {
                        try {
                            if (!IOHelper.isDir(Path.of(updatesDir)))
                                Files.createDirectory(Path.of(updatesDir));
                            sync(null);
                        } catch (IOException e) {
                            logger.error("Updates not synced", e);
                        }
                        try {
                            Path profilesDirPath = Path.of(profilesDir);
                            if (!IOHelper.isDir(profilesDirPath))
                                Files.createDirectory(profilesDirPath);
                            readProfilesDir();
                        } catch (IOException e) {
                            logger.error("Profiles not synced", e);
                        }
                        logger.info("Profiles and updates synced");
                    }
                }
        );
    }

    public class LocalProfile implements CompletedProfile {
        private volatile ClientProfile profile;
        private volatile HashedDir clientDir;
        private volatile HashedDir assetDir;
        private final Path configPath;

        public LocalProfile(ClientProfile profile, HashedDir clientDir, HashedDir assetDir) {
            this.profile = profile;
            this.clientDir = clientDir;
            this.assetDir = assetDir;
            this.configPath = Path.of(profilesDir).resolve(profile.getDir().concat(".json"));
        }

        public LocalProfile(ClientProfile profile, HashedDir clientDir, HashedDir assetDir, Path configPath) {
            this.profile = profile;
            this.clientDir = clientDir;
            this.assetDir = assetDir;
            this.configPath = configPath;
        }

        @Override
        public String getTag() {
            return null;
        }

        @Override
        public ClientProfile getProfile() {
            return profile;
        }

        @Override
        public HashedDir getClientDir() {
            return clientDir;
        }

        @Override
        public HashedDir getAssetDir() {
            return assetDir;
        }

        @Override
        public UUID getUuid() {
            return profile.getUUID();
        }

        @Override
        public String getName() {
            return profile.getTitle();
        }

        @Override
        public String getDescription() {
            return profile.getInfo();
        }

        @Override
        public String getDefaultTag() {
            return null;
        }

        public Path getConfigPath() {
            return configPath;
        }
    }

    private final class ProfilesFileVisitor extends SimpleFileVisitor<Path> {
        private final Map<UUID, LocalProfile> result;
        private final Logger logger = LogManager.getLogger();

        private ProfilesFileVisitor(Map<UUID, LocalProfile> result) {
            this.result = result;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            logger.info("Syncing '{}' profile", IOHelper.getFileName(file));

            // Read profile
            ClientProfile profile;
            try (BufferedReader reader = IOHelper.newReader(file)) {
                profile = Launcher.gsonManager.gson.fromJson(reader, ClientProfile.class);
            }
            profile.verify();

            LocalProfile localProfile = new LocalProfile(profile, getUpdatesDir(profile.getDir()), getUpdatesDir(profile.getAssetDir()), file);
            result.put(localProfile.getUuid(), localProfile);
            return super.visitFile(file, attrs);
        }
    }
}
