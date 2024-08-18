package pro.gravit.launchserver.command.profiles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.profiles.ClientProfile;
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
                ClientProfile profile;
                try {
                    UUID uuid = UUID.fromString(profileName);
                    profile = server.config.profileProvider.getProfile(uuid);
                } catch (IllegalArgumentException ex) {
                    profile = server.config.profileProvider.getProfile(profileName);
                }
                server.config.profileProvider.addProfile(profile);
            }
            server.syncProfilesDir();
        }
    }

}
