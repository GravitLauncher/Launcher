package pro.gravit.launchserver.command.hash;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.helper.MakeProfileHelper;
import pro.gravit.utils.helper.IOHelper;

import java.io.Writer;

public class MakeProfileCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public MakeProfileCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[name] [minecraft version] [dir]";
    }

    @Override
    public String getUsageDescription() {
        return "make profile for any minecraft versions";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 3);
        ClientProfile.Version version = ClientProfile.Version.byName(args[1]);
        MakeProfileHelper.MakeProfileOption[] options = MakeProfileHelper.getMakeProfileOptionsFromDir(server.updatesDir.resolve(args[2]), version);
        for (MakeProfileHelper.MakeProfileOption option : options) {
            logger.info("Detected option {}", option);
        }
        ClientProfile profile = MakeProfileHelper.makeProfile(ClientProfile.Version.byName(args[1]), args[0], options);
        try (Writer writer = IOHelper.newWriter(server.profilesDir.resolve(args[0].concat(".json")))) {
            Launcher.gsonManager.configGson.toJson(profile, writer);
        }
        logger.info("Profile {} created", args[0]);
        server.syncProfilesDir();
    }
}
