package pro.gravit.launchserver.command.profiles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.profiles.ProfilesProvider;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class CloneProfileCommand extends Command {
    private final transient Logger logger = LogManager.getLogger(CloneProfileCommand.class);
    public CloneProfileCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[profile title/uuid] [new profile title]";
    }

    @Override
    public String getUsageDescription() {
        return "clone profile and profile dir";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        ProfilesProvider.CompletedProfile profile;
        try {
            UUID uuid = UUID.fromString(args[0]);
            profile = server.config.profilesProvider.get(uuid, null);
        } catch (IllegalArgumentException ex) {
            profile = server.config.profilesProvider.get(args[0], null);
        }
        server.config.profilesProvider.create(args[1], "Description", profile);
        logger.info("Profile {} cloned from {}", args[1], args[0]);
    }
}
