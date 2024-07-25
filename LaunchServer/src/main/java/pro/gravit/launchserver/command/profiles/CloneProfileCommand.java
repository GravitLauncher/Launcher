package pro.gravit.launchserver.command.profiles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
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
        ClientProfile profile;
        try {
            UUID uuid = UUID.fromString(args[0]);
            profile = server.config.profileProvider.getProfile(uuid);
        } catch (IllegalArgumentException ex) {
            profile = server.config.profileProvider.getProfile(args[0]);
        }
        var builder = new ClientProfileBuilder(profile);
        builder.setTitle(args[1]);
        builder.setUuid(UUID.randomUUID());
        if(profile.getServers().size() == 1) {
            profile.getServers().getFirst().name = args[1];
        }
        logger.info("Copy {} to {}", profile.getDir(), args[1]);
        var src = server.updatesDir.resolve(profile.getDir());
        var dest = server.updatesDir.resolve(args[1]);
        try (Stream<Path> stream = Files.walk(src)) {
            stream.forEach(source -> {
                try {
                    IOHelper.copy(source, dest.resolve(src.relativize(source)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        builder.setDir(args[1]);
        profile = builder.createClientProfile();
        server.config.profileProvider.addProfile(profile);
        logger.info("Profile {} cloned from {}", args[1], args[0]);
        server.syncProfilesDir();
        server.syncUpdatesDir(List.of(args[1]));
    }
}
