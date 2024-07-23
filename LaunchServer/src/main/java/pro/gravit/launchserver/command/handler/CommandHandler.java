package pro.gravit.launchserver.command.handler;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.basic.*;
import pro.gravit.launchserver.command.hash.*;
import pro.gravit.launchserver.command.modules.LoadModuleCommand;
import pro.gravit.launchserver.command.modules.ModulesCommand;
import pro.gravit.launchserver.command.profiles.ProfilesCommand;
import pro.gravit.launchserver.command.service.*;
import pro.gravit.launchserver.command.sync.*;
import pro.gravit.launchserver.command.tools.SignDirCommand;
import pro.gravit.launchserver.command.tools.SignJarCommand;
import pro.gravit.utils.command.BaseCommandCategory;
import pro.gravit.utils.command.basic.ClearCommand;
import pro.gravit.utils.command.basic.GCCommand;
import pro.gravit.utils.command.basic.HelpCommand;

public abstract class CommandHandler extends pro.gravit.utils.command.CommandHandler {
    public static void registerCommands(pro.gravit.utils.command.CommandHandler handler, LaunchServer server) {
        BaseCommandCategory basic = new BaseCommandCategory();
        // Register basic commands
        basic.registerCommand("help", new HelpCommand(handler));
        basic.registerCommand("version", new VersionCommand(server));
        basic.registerCommand("build", new BuildCommand(server));
        basic.registerCommand("stop", new StopCommand(server));
        basic.registerCommand("debug", new DebugCommand(server));
        basic.registerCommand("clear", new ClearCommand(handler));
        basic.registerCommand("gc", new GCCommand());
        basic.registerCommand("loadModule", new LoadModuleCommand(server));
        basic.registerCommand("modules", new ModulesCommand(server));
        Category basicCategory = new Category(basic, "basic", "Base LaunchServer commands");
        handler.registerCategory(basicCategory);

        // Register sync commands
        BaseCommandCategory updates = new BaseCommandCategory();
        updates.registerCommand("indexAsset", new IndexAssetCommand(server));
        updates.registerCommand("unindexAsset", new UnindexAssetCommand(server));
        updates.registerCommand("downloadAsset", new DownloadAssetCommand(server));
        updates.registerCommand("downloadClient", new DownloadClientCommand(server));
        updates.registerCommand("sync", new SyncCommand(server));
        updates.registerCommand("profile", new ProfilesCommand(server));
        Category updatesCategory = new Category(updates, "updates", "Update and Sync Management");
        handler.registerCategory(updatesCategory);

        //Register service commands
        BaseCommandCategory service = new BaseCommandCategory();
        service.registerCommand("config", new ConfigCommand(server));
        service.registerCommand("serverStatus", new ServerStatusCommand(server));
        service.registerCommand("notify", new NotifyCommand(server));
        service.registerCommand("component", new ComponentCommand(server));
        service.registerCommand("clients", new ClientsCommand(server));
        service.registerCommand("securitycheck", new SecurityCheckCommand(server));
        service.registerCommand("token", new TokenCommand(server));
        Category serviceCategory = new Category(service, "service", "Managing LaunchServer Components");
        handler.registerCategory(serviceCategory);

        //Register tools commands
        BaseCommandCategory tools = new BaseCommandCategory();
        tools.registerCommand("signJar", new SignJarCommand(server));
        tools.registerCommand("signDir", new SignDirCommand(server));
        Category toolsCategory = new Category(tools, "tools", "Other tools");
        handler.registerCategory(toolsCategory);
    }
}
