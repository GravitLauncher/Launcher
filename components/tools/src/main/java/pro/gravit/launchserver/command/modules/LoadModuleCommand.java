package pro.gravit.launchserver.command.modules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LoadModuleCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public LoadModuleCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[name or jar]";
    }

    @Override
    public String getUsageDescription() {
        return "Module jar file";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        Path file = Paths.get(args[0]);
        String target = args[0];
        boolean bundled = false;
        if(!target.endsWith(".jar") && !Files.isDirectory(file)) {
            // It is in-bundle module
            file = server.modulesDir.resolve(target+".jar");
            bundled = true;
        }
        if(!Files.exists(file) && bundled) {
            target = target.concat("_module");
            file = server.modulesDir.resolve(target+".jar");
        }
        if(!Files.exists(file)) {
            throw new FileNotFoundException(file.toString());
        }
        if(Files.isDirectory(file)) {
            server.modulesManager.autoload(file);
        } else {
            server.modulesManager.loadModule(file);
        }
        if(!server.modulesConfig.loadModules.contains(target)) {
            server.modulesConfig.loadModules.add(target);
            server.launchServerConfigManager.writeModulesConfig(server.modulesConfig);
        }
        logger.info("Module {} loaded from {}", target, file);
    }
}
