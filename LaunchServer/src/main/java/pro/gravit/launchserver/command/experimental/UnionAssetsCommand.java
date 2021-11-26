package pro.gravit.launchserver.command.experimental;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class UnionAssetsCommand extends Command {
    public UnionAssetsCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[]";
    }

    @Override
    public String getUsageDescription() {
        return "union assets";
    }

    @Override
    public void invoke(String... args) throws Exception {
        List<String> checkedDirs = new ArrayList<>();
        var assetDir = server.updatesDir.resolve("assets");
        if(Files.notExists(assetDir)) {
            Files.createDirectories(assetDir);
        }
        for(ClientProfile profile : server.getProfiles()) {
            var visitor = new UnionLibrariesCommand.ListFileVisitor();
            if(checkedDirs.contains(profile.getAssetDir())) {
                continue;
            }
            var dir = server.updatesDir.resolve(profile.getAssetDir());
            IOHelper.walk(dir, visitor, false);
            var list = visitor.getList();
            for(Path p : list) {
                Path relativized = dir.relativize(p);
                Path target = assetDir.resolve(relativized);
                if(Files.notExists(target)) {
                    IOHelper.createParentDirs(target);
                    Files.move(p, target);
                }
            }
            IOHelper.deleteDir(dir, true);
            profile.setAssetDir("assets");
            checkedDirs.add(profile.getAssetDir());
            try (Writer w = IOHelper.newWriter(server.profilesDir.resolve(profile.getTitle().concat(".json")))) {
                Launcher.gsonManager.configGson.toJson(profile, w);
            }
        }
        server.syncProfilesDir();
        server.updatesManager.syncUpdatesDir(null);
    }
}
