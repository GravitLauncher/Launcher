package pro.gravit.launchserver.command.profiles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.profiles.ProfilesProvider;
import pro.gravit.launchserver.command.Command;

import java.util.UUID;

public class DeleteProfileCommand extends Command {
    private final transient Logger logger = LogManager.getLogger(ListProfilesCommand.class);
    public DeleteProfileCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[uuid/title]";
    }

    @Override
    public String getUsageDescription() {
        return "permanently delete profile";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
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
        logger.info("Delete {}", args[0]);
        server.config.profilesProvider.delete(profile);
    }
}
