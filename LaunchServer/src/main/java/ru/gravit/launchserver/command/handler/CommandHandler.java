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

public abstract class CommandHandler extends ru.gravit.utils.command.CommandHandler {
    public static void registerCommands(ru.gravit.utils.command.CommandHandler handler) {
        LaunchServer server = LaunchServer.server;
        // Register basic commands
        handler.registerCommand("help", new HelpCommand(server));
        handler.registerCommand("version", new VersionCommand(server));
        handler.registerCommand("build", new BuildCommand(server));
        handler.registerCommand("stop", new StopCommand(server));
        handler.registerCommand("restart", new RestartCommand(server));
        handler.registerCommand("rebind", new RebindCommand(server));
        handler.registerCommand("debug", new DebugCommand(server));
        handler.registerCommand("clear", new ClearCommand(server));
        handler.registerCommand("gc", new GCCommand(server));
        handler.registerCommand("proguardClean", new ProguardCleanCommand(server));
        handler.registerCommand("proguardDictRegen", new RegenProguardDictCommand(server));
        handler.registerCommand("proguardMappingsRemove", new RemoveMappingsProguardCommand(server));
        handler.registerCommand("logConnections", new LogConnectionsCommand(server));
        handler.registerCommand("loadModule", new LoadModuleCommand(server));
        handler.registerCommand("modules", new ModulesCommand(server));
        handler.registerCommand("test", new TestCommand(server));

        // Register sync commands
        handler.registerCommand("indexAsset", new IndexAssetCommand(server));
        handler.registerCommand("unindexAsset", new UnindexAssetCommand(server));
        handler.registerCommand("downloadAsset", new DownloadAssetCommand(server));
        handler.registerCommand("downloadClient", new DownloadClientCommand(server));
        handler.registerCommand("syncBinaries", new SyncBinariesCommand(server));
        handler.registerCommand("syncUpdates", new SyncUpdatesCommand(server));
        handler.registerCommand("syncProfiles", new SyncProfilesCommand(server));

        // Register auth commands
        handler.registerCommand("auth", new AuthCommand(server));
        handler.registerCommand("usernameToUUID", new UsernameToUUIDCommand(server));
        handler.registerCommand("uuidToUsername", new UUIDToUsernameCommand(server));
        handler.registerCommand("ban", new BanCommand(server));
        handler.registerCommand("unban", new UnbanCommand(server));
        handler.registerCommand("getHWID", new GetHWIDCommand(server));

        //Register dump commands
        handler.registerCommand("dumpSessions", new DumpSessionsCommand(server));
        handler.registerCommand("dumpEntryCache", new DumpEntryCacheCommand(server));

        //Register service commands
        handler.registerCommand("reload", new ReloadCommand(server));
        handler.registerCommand("reloadAll", new ReloadAllCommand(server));
        handler.registerCommand("reloadList", new ReloadListCommand(server));
        handler.registerCommand("config", new ConfigCommand(server));
        handler.registerCommand("configHelp", new ConfigHelpCommand(server));
        handler.registerCommand("configList", new ConfigListCommand(server));
        handler.registerCommand("serverStatus", new ServerStatusCommand(server));
        handler.registerCommand("checkInstall", new CheckInstallCommand(server));
        handler.registerCommand("multi", new MultiCommand(server));
        handler.registerCommand("getModulus", new GetModulusCommand(server));
        handler.registerCommand("component", new ComponentCommand(server));
        handler.registerCommand("givePermission", new GivePermissionsCommand(server));
        handler.registerCommand("getPermissions", new GetPermissionsCommand(server));
    }
}
