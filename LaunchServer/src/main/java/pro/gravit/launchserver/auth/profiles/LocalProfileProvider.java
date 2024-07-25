package pro.gravit.launchserver.auth.profiles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.utils.helper.IOHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class LocalProfileProvider extends ProfileProvider {
    public String profilesDir = "profiles";
    private transient volatile Map<Path, ClientProfile> profilesMap;
    private transient volatile Set<ClientProfile> profilesList; // Cache
    @Override
    public void sync() throws IOException {
        Path profilesDirPath = Path.of(profilesDir);
        if (!IOHelper.isDir(profilesDirPath))
            Files.createDirectory(profilesDirPath);
        Map<Path, ClientProfile> newProfiles = new HashMap<>();
        IOHelper.walk(profilesDirPath, new ProfilesFileVisitor(newProfiles), false);
        Set<ClientProfile> newProfilesList = new HashSet<>(newProfiles.values());
        profilesMap = newProfiles;
        profilesList = newProfilesList;
    }

    @Override
    public Set<ClientProfile> getProfiles() {
        return profilesList;
    }

    @Override
    public void addProfile(ClientProfile profile) throws IOException {
        Path profilesDirPath = Path.of(profilesDir);
        Path target = IOHelper.resolveIncremental(profilesDirPath,
                profile.getDir(), "json");
        try (BufferedWriter writer = IOHelper.newWriter(target)) {
            Launcher.gsonManager.configGson.toJson(profile, writer);
        }
        addProfile(target, profile);
    }

    @Override
    public void deleteProfile(ClientProfile profile) throws IOException {
        for(var e : profilesMap.entrySet()) {
            if(e.getValue().getUUID().equals(profile.getUUID())) {
                Files.deleteIfExists(e.getKey());
                profilesMap.remove(e.getKey());
                profilesList.remove(e.getValue());
                break;
            }
        }
    }

    private void addProfile(Path path, ClientProfile profile) {
        profilesMap.put(path, profile);
        profilesList.add(profile);
    }

    private static final class ProfilesFileVisitor extends SimpleFileVisitor<Path> {
        private final Map<Path, ClientProfile> result;
        private final Logger logger = LogManager.getLogger();

        private ProfilesFileVisitor(Map<Path, ClientProfile> result) {
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
            profile.setProfileFilePath(file);

            // Add SIGNED profile to result list
            result.put(file, profile);
            return super.visitFile(file, attrs);
        }
    }
}
