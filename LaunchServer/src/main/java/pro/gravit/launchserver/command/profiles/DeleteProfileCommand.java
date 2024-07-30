package pro.gravit.launchserver.command.profiles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.nio.file.Files;
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
        ClientProfile profile;
        try {
            UUID uuid = UUID.fromString(args[0]);
            profile = server.config.profileProvider.getProfile(uuid);
        } catch (IllegalArgumentException ex) {
            profile = server.config.profileProvider.getProfile(args[0]);
        }
        if(profile == null) {
            logger.error("Profile {} not found", args[0]);
            return;
        }
        logger.warn("THIS ACTION DELETE PROFILE AND ALL FILES IN {}", profile.getDir());
        if(!showApplyDialog("Continue?")) {
            return;
        }
        logger.info("Delete {} ({})", profile.getTitle(), profile.getUUID());
        server.config.profileProvider.deleteProfile(profile);
        logger.info("Delete {}", profile.getDir());
        server.config.updatesProvider.delete(profile.getDir());
    }
}
