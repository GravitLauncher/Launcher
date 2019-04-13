package ru.gravit.launchserver.command.handler;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.auth.*;
import ru.gravit.launchserver.command.basic.*;
import ru.gravit.launchserver.command.dump.DumpEntryCacheCommand;
import ru.gravit.launchserver.command.dump.DumpSessionsCommand;
import ru.gravit.launchserver.command.hash.*;
import ru.gravit.launchserver.command.install.CheckInstallCommand;
import ru.gravit.launchserver.command.install.MultiCommand;
import ru.gravit.launchserver.command.modules.LoadModuleCommand;
import ru.gravit.launchserver.command.modules.ModulesCommand;
import ru.gravit.launchserver.command.service.*;
import ru.gravit.utils.command.BaseCommandCategory;
import ru.gravit.utils.command.basic.HelpCommand;

public abstract class CommandHandler extends ru.gravit.utils.command.CommandHandler {
    public static void registerCommands(ru.gravit.utils.command.CommandHandler handler) {
        LaunchServer server = LaunchServer.server;
        BaseCommandCategory basic = new BaseCommandCategory();
        // Register basic commands
        basic.registerCommand("help", new HelpCommand(handler));
        basic.registerCommand("version", new VersionCommand(server));
        basic.registerCommand("build", new BuildCommand(server));
        basic.registerCommand("stop", new StopCommand(server));
        basic.registerCommand("restart", new RestartCommand(server));
        basic.registerCommand("rebind", new RebindCommand(server));
        basic.registerCommand("debug", new DebugCommand(server));
        basic.registerCommand("clear", new ClearCommand(server));
        basic.registerCommand("gc", new GCCommand(server));
        basic.registerCommand("proguardClean", new ProguardCleanCommand(server));
        basic.registerCommand("proguardDictRegen", new RegenProguardDictCommand(server));
        basic.registerCommand("proguardMappingsRemove", new RemoveMappingsProguardCommand(server));
        basic.registerCommand("logConnections", new LogConnectionsCommand(server));
        basic.registerCommand("loadModule", new LoadModuleCommand(server));
        basic.registerCommand("modules", new ModulesCommand(server));
        basic.registerCommand("test", new TestCommand(server));
        Category basicCategory = new Category(basic,"basic", "Base LaunchServer commands");
        handler.registerCategory(basicCategory);

        // Register sync commands
        BaseCommandCategory updates = new BaseCommandCategory();
        updates.registerCommand("indexAsset", new IndexAssetCommand(server));
        updates.registerCommand("unindexAsset", new UnindexAssetCommand(server));
        updates.registerCommand("downloadAsset", new DownloadAssetCommand(server));
        updates.registerCommand("downloadClient", new DownloadClientCommand(server));
        updates.registerCommand("syncBinaries", new SyncBinariesCommand(server));
        updates.registerCommand("syncUpdates", new SyncUpdatesCommand(server));
        updates.registerCommand("syncProfiles", new SyncProfilesCommand(server));
        Category updatesCategory = new Category(updates,"updates", "Update and Sync Management");
        handler.registerCategory(updatesCategory);

        // Register auth commands
        BaseCommandCategory auth = new BaseCommandCategory();
        auth.registerCommand("auth", new AuthCommand(server));
        auth.registerCommand("usernameToUUID", new UsernameToUUIDCommand(server));
        auth.registerCommand("uuidToUsername", new UUIDToUsernameCommand(server));
        auth.registerCommand("ban", new BanCommand(server));
        auth.registerCommand("unban", new UnbanCommand(server));
        auth.registerCommand("getHWID", new GetHWIDCommand(server));
        Category authCategory = new Category(auth,"auth", "User Management");
        handler.registerCategory(authCategory);

        //Register dump commands
        BaseCommandCategory dump = new BaseCommandCategory();
        dump.registerCommand("dumpSessions", new DumpSessionsCommand(server));
        dump.registerCommand("dumpEntryCache", new DumpEntryCacheCommand(server));
        Category dumpCategory = new Category(dump,"dump", "Dump runtime data");
        handler.registerCategory(dumpCategory);

        //Register service commands
        BaseCommandCategory service = new BaseCommandCategory();
        service.registerCommand("reload", new ReloadCommand(server));
        service.registerCommand("reloadAll", new ReloadAllCommand(server));
        service.registerCommand("reloadList", new ReloadListCommand(server));
        service.registerCommand("config", new ConfigCommand(server));
        service.registerCommand("configHelp", new ConfigHelpCommand(server));
        service.registerCommand("configList", new ConfigListCommand(server));
        service.registerCommand("serverStatus", new ServerStatusCommand(server));
        service.registerCommand("checkInstall", new CheckInstallCommand(server));
        service.registerCommand("multi", new MultiCommand(server));
        service.registerCommand("getModulus", new GetModulusCommand(server));
        service.registerCommand("component", new ComponentCommand(server));
        service.registerCommand("givePermission", new GivePermissionsCommand(server));
        service.registerCommand("getPermissions", new GetPermissionsCommand(server));
        Category serviceCategory = new Category(service,"service", "Managing LaunchServer Components");
        handler.registerCategory(serviceCategory);
    }
}
