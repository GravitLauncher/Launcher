package pro.gravit.launchserver.command.hash;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class SaveProfilesCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public SaveProfilesCommand(LaunchServer server) {
        super(server);
    }

    public static void saveProfile(ClientProfile profile, Path path) throws IOException {
        if (profile.getUUID() == null) profile.setUUID(UUID.randomUUID());
        if (profile.getServers().size() == 0) {
            ClientProfile.ServerProfile serverProfile = new ClientProfile.ServerProfile();
            serverProfile.isDefault = true;
            serverProfile.name = profile.getTitle();
            serverProfile.serverAddress = profile.getServerAddress();
            serverProfile.serverPort = profile.getServerPort();
            profile.getServers().add(serverProfile);
        }
        try (Writer w = IOHelper.newWriter(path)) {
            Launcher.gsonManager.configGson.toJson(profile, w);
        }
    }

    @Override
    public String getArgsDescription() {
        return "[profile names...]";
    }

    @Override
    public String getUsageDescription() {
        return "load and save profile";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        if (args.length > 0) {
            for (String profileName : args) {
                Path profilePath = server.profilesDir.resolve(profileName.concat(".json"));
                if (!Files.exists(profilePath)) {
                    logger.error("Profile {} not found", profilePath.toString());
                    return;
                }
                ClientProfile profile;
                try (Reader reader = IOHelper.newReader(profilePath)) {
                    profile = Launcher.gsonManager.configGson.fromJson(reader, ClientProfile.class);
                }
                saveProfile(profile, profilePath);
                logger.info("Profile {} save successful", profilePath.toString());
            }
            server.syncProfilesDir();
        }
    }

}
