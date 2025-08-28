package pro.gravit.launchserver.command.profiles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class ListProfilesCommand extends Command {
    private final transient Logger logger = LogManager.getLogger(ListProfilesCommand.class);
    public ListProfilesCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "show all profiles";
    }

    @Override
    public void invoke(String... args) {
        for(var profile : server.config.profilesProvider.getProfiles(null)) {
            logger.info("{} ({})", profile.getName(), profile.getUuid());
        }
    }
}
