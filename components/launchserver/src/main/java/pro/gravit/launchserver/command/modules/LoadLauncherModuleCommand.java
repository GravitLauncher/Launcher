package pro.gravit.launchserver.command.modules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jline.reader.Candidate;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LoadLauncherModuleCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public LoadLauncherModuleCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[name or jar]";
    }

    @Override
    public String getUsageDescription() {
        return "Launcher module jar file";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        Path file = Paths.get(args[0]);
        String target = args[0];
        boolean bundled = false;
        if(!target.endsWith(".jar") && !Files.isDirectory(file)) {
            // It is in-bundle module
            file = server.modulesDir.resolve(args[0]+".jar");
            bundled = true;
        }
        if(!Files.exists(file) && bundled) {
            target = target.concat("_lmodule");
            file = server.modulesDir.resolve(target+".jar");
        }
        if(!Files.exists(file)) {
            throw new FileNotFoundException(file.toString());
        }
        if(Files.isDirectory(file)) {
            server.launcherModuleLoader.registerModule(file);
        } else {
            server.launcherModuleLoader.autoload(file);
        }
        if(!server.modulesConfig.loadLauncherModules.contains(target)) {
            server.modulesConfig.loadLauncherModules.add(target);
            server.launchServerConfigManager.writeModulesConfig(server.modulesConfig);
        }
        logger.info("Launcher module {} loaded from {}", target, file);
    }

    @Override
    public List<Candidate> complete(List<String> words, int wordIndex, String word) {
        if (wordIndex == 0) {
            List<Candidate> candidates = new ArrayList<>();
            try (var stream = Files.list(server.modulesDir)) {
                stream.forEach(p -> {
                    String file = p.getFileName().toString();
                    if (!file.endsWith("_lmodule.jar")) return;
                    String name = file.substring(0, file.length() - "_lmodule.jar".length());
                    if (name.startsWith(word)) candidates.add(new Candidate(name));
                });
            } catch (IOException ignored) {}
            return candidates;
        }
        return new ArrayList<>();
    }
}
