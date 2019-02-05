package ru.gravit.launchserver.command.handler;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.launchserver.command.CommandException;
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
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CommandHandler implements Runnable {
    private static String[] parse(CharSequence line) throws CommandException {
        boolean quoted = false;
        boolean wasQuoted = false;

        // Read line char by char
        Collection<String> result = new LinkedList<>();
        StringBuilder builder = new StringBuilder(100);
        for (int i = 0; i <= line.length(); i++) {
            boolean end = i >= line.length();
            char ch = end ? '\0' : line.charAt(i);

            // Maybe we should read next argument?
            if (end || !quoted && Character.isWhitespace(ch)) {
                if (end && quoted)
                    throw new CommandException("Quotes wasn't closed");

                // Empty args are ignored (except if was quoted)
                if (wasQuoted || builder.length() > 0)
                    result.add(builder.toString());

                // Reset file builder
                wasQuoted = false;
                builder.setLength(0);
                continue;
            }

            // Append next char
            switch (ch) {
                case '"': // "abc"de, "abc""de" also allowed
                    quoted = !quoted;
                    wasQuoted = true;
                    break;
                case '\\': // All escapes, including spaces etc
                    if (i + 1 >= line.length())
                        throw new CommandException("Escape character is not specified");
                    char next = line.charAt(i + 1);
                    builder.append(next);
                    i++;
                    break;
                default: // Default char, simply append
                    builder.append(ch);
                    break;
            }
        }

        // Return result as array
        return result.toArray(new String[0]);
    }

    private final Map<String, Command> commands = new ConcurrentHashMap<>(32);

    protected CommandHandler(LaunchServer server) {
        // Register basic commands
        registerCommand("help", new HelpCommand(server));
        registerCommand("version", new VersionCommand(server));
        registerCommand("build", new BuildCommand(server));
        registerCommand("stop", new StopCommand(server));
        registerCommand("restart", new RestartCommand(server));
        registerCommand("rebind", new RebindCommand(server));
        registerCommand("debug", new DebugCommand(server));
        registerCommand("clear", new ClearCommand(server));
        registerCommand("gc", new GCCommand(server));
        registerCommand("proguardClean", new ProguardCleanCommand(server));
        registerCommand("proguardDictRegen", new RegenProguardDictCommand(server));
        registerCommand("proguardMappingsRemove", new RemoveMappingsProguardCommand(server));
        registerCommand("logConnections", new LogConnectionsCommand(server));
        registerCommand("loadModule", new LoadModuleCommand(server));
        registerCommand("modules", new ModulesCommand(server));
        registerCommand("test", new TestCommand(server));

        // Register sync commands
        registerCommand("indexAsset", new IndexAssetCommand(server));
        registerCommand("unindexAsset", new UnindexAssetCommand(server));
        registerCommand("downloadAsset", new DownloadAssetCommand(server));
        registerCommand("downloadClient", new DownloadClientCommand(server));
        registerCommand("syncBinaries", new SyncBinariesCommand(server));
        registerCommand("syncUpdates", new SyncUpdatesCommand(server));
        registerCommand("syncProfiles", new SyncProfilesCommand(server));

        // Register auth commands
        registerCommand("auth", new AuthCommand(server));
        registerCommand("usernameToUUID", new UsernameToUUIDCommand(server));
        registerCommand("uuidToUsername", new UUIDToUsernameCommand(server));
        registerCommand("ban", new BanCommand(server));
        registerCommand("unban", new UnbanCommand(server));

        //Register dump commands
        registerCommand("dumpSessions", new DumpSessionsCommand(server));
        registerCommand("dumpEntryCache", new DumpEntryCacheCommand(server));

        //Register service commands
        registerCommand("reload", new ReloadCommand(server));
        registerCommand("reloadAll", new ReloadAllCommand(server));
        registerCommand("reloadList", new ReloadListCommand(server));
        registerCommand("config", new ConfigCommand(server));
        registerCommand("configHelp", new ConfigHelpCommand(server));
        registerCommand("configList", new ConfigListCommand(server));
        registerCommand("swapAuthProvider", new SwapAuthProviderCommand(server));
        registerCommand("serverStatus", new ServerStatusCommand(server));
        registerCommand("checkInstall", new CheckInstallCommand(server));
        registerCommand("multi", new MultiCommand(server));
    }


    public abstract void bell() throws IOException;


    public abstract void clear() throws IOException;


    public final Map<String, Command> commandsMap() {
        return Collections.unmodifiableMap(commands);
    }


    public final void eval(String line, boolean bell) {
        LogHelper.info("Command '%s'", line);

        // Parse line to tokens
        String[] args;
        try {
            args = parse(line);
        } catch (Exception e) {
            LogHelper.error(e);
            return;
        }

        // Evaluate command
        eval(args, bell);
    }


    public final void eval(String[] args, boolean bell) {
        if (args.length == 0)
            return;

        // Measure start time and invoke command
        Instant startTime = Instant.now();
        try {
            lookup(args[0]).invoke(Arrays.copyOfRange(args, 1, args.length));
        } catch (Exception e) {
            LogHelper.error(e);
        }

        // Bell if invocation took > 1s
        Instant endTime = Instant.now();
        if (bell && Duration.between(startTime, endTime).getSeconds() >= 5)
            try {
                bell();
            } catch (IOException e) {
                LogHelper.error(e);
            }
    }


    public final Command lookup(String name) throws CommandException {
        Command command = commands.get(name);
        if (command == null)
            throw new CommandException(String.format("Unknown command: '%s'", name));
        return command;
    }


    public abstract String readLine() throws IOException;

    private void readLoop() throws IOException {
        for (String line = readLine(); line != null; line = readLine())
            eval(line, true);
    }


    public final void registerCommand(String name, Command command) {
        VerifyHelper.verifyIDName(name);
        VerifyHelper.putIfAbsent(commands, name, Objects.requireNonNull(command, "command"),
                String.format("Command has been already registered: '%s'", name));
    }

    @Override
    public final void run() {
        try {
            readLoop();
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }
}
