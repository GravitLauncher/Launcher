package pro.gravit.launchserver.command.handler;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.auth.AuthCommand;
import pro.gravit.launchserver.command.auth.BanCommand;
import pro.gravit.launchserver.command.auth.GetHWIDCommand;
import pro.gravit.launchserver.command.auth.UUIDToUsernameCommand;
import pro.gravit.launchserver.command.auth.UnbanCommand;
import pro.gravit.launchserver.command.auth.UsernameToUUIDCommand;
import pro.gravit.launchserver.command.basic.BuildCommand;
import pro.gravit.launchserver.command.basic.LogConnectionsCommand;
import pro.gravit.launchserver.command.basic.ProguardCleanCommand;
import pro.gravit.launchserver.command.basic.RebindCommand;
import pro.gravit.launchserver.command.basic.RegenProguardDictCommand;
import pro.gravit.launchserver.command.basic.RemoveMappingsProguardCommand;
import pro.gravit.launchserver.command.basic.RestartCommand;
import pro.gravit.launchserver.command.basic.StopCommand;
import pro.gravit.launchserver.command.basic.TestCommand;
import pro.gravit.launchserver.command.basic.VersionCommand;
import pro.gravit.launchserver.command.dao.GetAllUsersCommand;
import pro.gravit.launchserver.command.dao.GetUserCommand;
import pro.gravit.launchserver.command.dao.RegisterCommand;
import pro.gravit.launchserver.command.dao.SetUserPasswordCommand;
import pro.gravit.launchserver.command.dump.DumpEntryCacheCommand;
import pro.gravit.launchserver.command.dump.DumpSessionsCommand;
import pro.gravit.launchserver.command.hash.DownloadAssetCommand;
import pro.gravit.launchserver.command.hash.DownloadClientCommand;
import pro.gravit.launchserver.command.hash.IndexAssetCommand;
import pro.gravit.launchserver.command.hash.SyncBinariesCommand;
import pro.gravit.launchserver.command.hash.SyncProfilesCommand;
import pro.gravit.launchserver.command.hash.SyncUpdatesCommand;
import pro.gravit.launchserver.command.hash.UnindexAssetCommand;
import pro.gravit.launchserver.command.install.CheckInstallCommand;
import pro.gravit.launchserver.command.install.MultiCommand;
import pro.gravit.launchserver.command.modules.LoadModuleCommand;
import pro.gravit.launchserver.command.modules.ModulesCommand;
import pro.gravit.launchserver.command.service.ComponentCommand;
import pro.gravit.launchserver.command.service.ConfigCommand;
import pro.gravit.launchserver.command.service.ConfigHelpCommand;
import pro.gravit.launchserver.command.service.ConfigListCommand;
import pro.gravit.launchserver.command.service.GetModulusCommand;
import pro.gravit.launchserver.command.service.GetPermissionsCommand;
import pro.gravit.launchserver.command.service.GivePermissionsCommand;
import pro.gravit.launchserver.command.service.ReloadAllCommand;
import pro.gravit.launchserver.command.service.ReloadCommand;
import pro.gravit.launchserver.command.service.ReloadListCommand;
import pro.gravit.launchserver.command.service.ServerStatusCommand;
import pro.gravit.utils.command.BaseCommandCategory;
import pro.gravit.utils.command.basic.ClearCommand;
import pro.gravit.utils.command.basic.DebugCommand;
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
        basic.registerCommand("restart", new RestartCommand(server));
        basic.registerCommand("rebind", new RebindCommand(server));
        basic.registerCommand("debug", new DebugCommand());
        basic.registerCommand("clear", new ClearCommand(handler));
        basic.registerCommand("gc", new GCCommand());
        basic.registerCommand("proguardClean", new ProguardCleanCommand(server));
        basic.registerCommand("proguardDictRegen", new RegenProguardDictCommand(server));
        basic.registerCommand("proguardMappingsRemove", new RemoveMappingsProguardCommand(server));
        basic.registerCommand("logConnections", new LogConnectionsCommand(server));
        basic.registerCommand("loadModule", new LoadModuleCommand(server));
        basic.registerCommand("modules", new ModulesCommand(server));
        basic.registerCommand("test", new TestCommand(server));
        Category basicCategory = new Category(basic, "basic", "Base LaunchServer commands");
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
        Category updatesCategory = new Category(updates, "updates", "Update and Sync Management");
        handler.registerCategory(updatesCategory);

        //Register dao commands
        BaseCommandCategory dao = new BaseCommandCategory();
        dao.registerCommand("register", new RegisterCommand(server));
        dao.registerCommand("setUserPassword", new SetUserPasswordCommand(server));
        dao.registerCommand("getUser", new GetUserCommand(server));
        dao.registerCommand("getAllUsers", new GetAllUsersCommand(server));
        Category daoCategory = new Category(dao, "DAO", "Data Management");
        handler.registerCategory(daoCategory);

        // Register auth commands
        BaseCommandCategory auth = new BaseCommandCategory();
        auth.registerCommand("auth", new AuthCommand(server));
        auth.registerCommand("usernameToUUID", new UsernameToUUIDCommand(server));
        auth.registerCommand("uuidToUsername", new UUIDToUsernameCommand(server));
        auth.registerCommand("ban", new BanCommand(server));
        auth.registerCommand("unban", new UnbanCommand(server));
        auth.registerCommand("getHWID", new GetHWIDCommand(server));
        Category authCategory = new Category(auth, "auth", "User Management");
        handler.registerCategory(authCategory);

        //Register dump commands
        BaseCommandCategory dump = new BaseCommandCategory();
        dump.registerCommand("dumpSessions", new DumpSessionsCommand(server));
        dump.registerCommand("dumpEntryCache", new DumpEntryCacheCommand(server));
        Category dumpCategory = new Category(dump, "dump", "Dump runtime data");
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
        Category serviceCategory = new Category(service, "service", "Managing LaunchServer Components");
        handler.registerCategory(serviceCategory);
    }
}
