package pro.gravit.launchserver.command.hash;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.optional.OptionalFile;
import pro.gravit.launcher.profiles.optional.actions.*;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

public class SaveProfilesCommand extends Command {
    public SaveProfilesCommand(LaunchServer server) {
        super(server);
    }

    @SuppressWarnings("deprecation")
    public static void saveProfile(ClientProfile profile, Path path) throws IOException {
        if (profile.getUUID() == null) profile.setUUID(UUID.randomUUID());
        if (profile.getServers().size() == 0) {
            ClientProfile.ServerProfile serverProfile = new ClientProfile.ServerProfile();
            serverProfile.isDefault = true;
            serverProfile.name = profile.getTitle();
            serverProfile.serverAddress = profile.getServerAddress();
            serverProfile.serverPort = profile.getServerPort();
            profile.getServers().add(serverProfile);
        }
        for (OptionalFile file : profile.getOptional()) {
            if (file.list != null) {
                String[] list = file.list;
                file.list = null;
                if (file.actions == null) file.actions = new ArrayList<>(2);
                OptionalAction action;
                switch (file.type) {
                    case FILE:
                        OptionalActionFile result = new OptionalActionFile(new HashMap<>());
                        for (String s : list) result.files.put(s, "");
                        action = result;
                        break;
                    case CLASSPATH:
                        action = new OptionalActionClassPath(list);
                        break;
                    case JVMARGS:
                        action = new OptionalActionJvmArgs(Arrays.asList(list));
                        break;
                    case CLIENTARGS:
                        action = new OptionalActionClientArgs(Arrays.asList(list));
                        break;
                    default:
                        LogHelper.warning("Not converted optional %s with type %s. Type unknown", file.name, file.type.toString());
                        continue;
                }
                file.actions.add(action);
            }
        }
        try (Writer w = IOHelper.newWriter(path)) {
            Launcher.gsonManager.configGson.toJson(profile, w);
        }
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
                Path profilePath = server.profilesDir.resolve(profileName.concat(".json"));
                if (!Files.exists(profilePath)) {
                    LogHelper.error("Profile %s not found", profilePath.toString());
                    return;
                }
                ClientProfile profile;
                try (Reader reader = IOHelper.newReader(profilePath)) {
                    profile = Launcher.gsonManager.configGson.fromJson(reader, ClientProfile.class);
                }
                saveProfile(profile, profilePath);
                LogHelper.info("Profile %s save successful", profilePath.toString());
            }
            server.syncProfilesDir();
        }
    }
}
