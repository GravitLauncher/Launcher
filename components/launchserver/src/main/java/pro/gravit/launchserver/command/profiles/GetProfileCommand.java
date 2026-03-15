package pro.gravit.launchserver.command.profiles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.profiles.ProfilesProvider;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class GetProfileCommand extends Command {
    private final transient Logger logger = LogManager.getLogger(GetProfileCommand.class);
    protected GetProfileCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[uuid/title] [profile or null] (clientDir or null) (assetsDir or null)";
    }

    @Override
    public String getUsageDescription() {
        return "Download profile from remote storage";
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
        if(!args[1].equals("null")) {
            try(Writer writer = IOHelper.newWriter(Path.of(args[1]))) {
                Launcher.gsonManager.configGson.toJson(profile.getProfile(), writer);
            }
        }
        if(!args[2].equals("null")) {
            server.config.profilesProvider.download(profile, Map.of("", Path.of(args[2])), false);
        }
        if(!args[3].equals("null")) {
            server.config.profilesProvider.download(profile, Map.of("", Path.of(args[3])), true);
        }
    }
}
