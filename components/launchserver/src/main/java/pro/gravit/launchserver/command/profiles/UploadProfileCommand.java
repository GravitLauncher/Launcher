package pro.gravit.launchserver.command.profiles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.profiles.ProfilesProvider;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.io.Reader;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class UploadProfileCommand extends Command {
    private final transient Logger logger = LogManager.getLogger(GetProfileCommand.class);
    protected UploadProfileCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[uuid/title] [profile or null] (clientDir or null) (assetDir or null)";
    }

    @Override
    public String getUsageDescription() {
        return "Upload profile to remote storage";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 3);
        ProfilesProvider.CompletedProfile profile;
        try {
            UUID uuid = UUID.fromString(args[0]);
            profile = server.config.profilesProvider.get(uuid, null);
        } catch (IllegalArgumentException ex) {
            profile = server.config.profilesProvider.get(args[0], null);
        }
        if(profile == null) {
            logger.error("Profile {} not found", args[0]);
            return;
        }
        logger.info("Parsing clientProfile");
        ClientProfile clientProfile = profile.getProfile();
        if(!args[1].equals("null")) {
            try(Reader reader = IOHelper.newReader(Path.of(args[1]))) {
                clientProfile = Launcher.gsonManager.configGson.fromJson(reader, ClientProfile.class);
            }
        }
        logger.info("Upload new version");
        server.config.profilesProvider.pushUpdate(profile, null, clientProfile,
                args[3].equals("null") ? null : List.of(ProfilesProvider.ProfileAction.upload(Path.of(args[3]), "", false)),
                        args[2].equals("null") ? null : List.of(ProfilesProvider.ProfileAction.upload(Path.of(args[2]), "", false)),
                                List.of());
        logger.info("Completed");
    }
}
