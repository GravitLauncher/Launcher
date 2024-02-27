package pro.gravit.launchserver.command.profiles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.nio.file.Files;

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
        ClientProfile profile = null;
        for(var p : server.getProfiles()) {
            if(p.getUUID().toString().equals(args[0]) || p.getTitle().equals(args[1])) {
                profile = p;
                break;
            }
        }
        if(profile == null) {
            logger.error("Profile {} not found", args[0]);
            return;
        }
        var clientDir = server.updatesDir.resolve(profile.getDir()).toAbsolutePath();
        logger.warn("THIS ACTION DELETE PROFILE AND ALL FILES IN {}", clientDir);
        if(!showApplyDialog("Continue?")) {
            return;
        }
        logger.info("Delete {}", clientDir);
        IOHelper.deleteDir(clientDir, true);
        var profileFile = profile.getProfileFilePath();
        if(profileFile == null) {
            profileFile = server.profilesDir.resolve(profile.getTitle().concat(".json"));
        }
        logger.info("Delete {}", profileFile);
        Files.deleteIfExists(profileFile);
    }
}
