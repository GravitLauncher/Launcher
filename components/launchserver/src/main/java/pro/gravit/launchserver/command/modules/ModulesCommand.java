package pro.gravit.launchserver.command.modules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.core.LauncherTrustManager;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfo;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.launchermodules.LauncherModuleLoader;

import java.security.cert.X509Certificate;
import java.util.Arrays;

public class ModulesCommand extends Command {

    public ModulesCommand(LaunchServer server) {
        super(server);
        childCommands.put("list", new ModulesListCommand(server));
        childCommands.put("load", new LoadModuleCommand(server));
        childCommands.put("launcher-load", new LoadLauncherModuleCommand(server));
        childCommands.put("launcher-reload", new ReloadLauncherModuleCommand(server));
    }

    @Override
    public String getArgsDescription() {
        return "[subcommand] [args...]";
    }

    @Override
    public String getUsageDescription() {
        return "add/remove and list modules";
    }

    @Override
    public void invoke(String... args) throws Exception {
        invokeSubcommands(args);
    }
}
