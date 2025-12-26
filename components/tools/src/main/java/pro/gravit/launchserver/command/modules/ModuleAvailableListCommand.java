package pro.gravit.launchserver.command.modules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfo;
import pro.gravit.launcher.core.LauncherTrustManager;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.launchermodules.LauncherModuleLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.stream.Stream;

public class ModuleAvailableListCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public ModuleAvailableListCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "list available modules";
    }

    @Override
    public void invoke(String... args) throws IOException {
        try(var stream = Files.list(server.modulesDir)) {
            stream.forEach(p -> {
                var file = p.getFileName().toString();
                if(!file.endsWith(".jar")) {
                    return;
                }
                file = file.substring(0, file.length() - ".jar".length());
                if(file.endsWith("_module")) {
                    file = file.substring(0, file.length() - "_module".length());
                    logger.info("Found LaunchServer module \t{}", file);
                } else if(file.endsWith("_lmodule")) {
                    file = file.substring(0, file.length() - "_lmodule".length());
                    logger.info("Found launcher module \t{}", file);
                } else {
                    logger.info("Found unknown module \t{}", file);
                }
            });
        }
    }
}
