package ru.gravit.launchserver.command.handler;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.helper.LogHelper;
import ru.gravit.launcher.helper.VerifyHelper;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.launchserver.command.CommandException;
import ru.gravit.launchserver.command.auth.AuthCommand;
import ru.gravit.launchserver.command.auth.BanCommand;
import ru.gravit.launchserver.command.auth.UUIDToUsernameCommand;
import ru.gravit.launchserver.command.auth.UnbanCommand;
import ru.gravit.launchserver.command.auth.UsernameToUUIDCommand;
import ru.gravit.launchserver.command.basic.BuildCommand;
import ru.gravit.launchserver.command.basic.ClearCommand;
import ru.gravit.launchserver.command.basic.DebugCommand;
import ru.gravit.launchserver.command.basic.GCCommand;
import ru.gravit.launchserver.command.basic.HelpCommand;
import ru.gravit.launchserver.command.basic.LogConnectionsCommand;
import ru.gravit.launchserver.command.basic.ProguardCleanCommand;
import ru.gravit.launchserver.command.basic.RebindCommand;
import ru.gravit.launchserver.command.basic.RegenProguardDictCommand;
import ru.gravit.launchserver.command.basic.RemoveMappingsProguardCommand;
import ru.gravit.launchserver.command.basic.StopCommand;
import ru.gravit.launchserver.command.basic.VersionCommand;
import ru.gravit.launchserver.command.hash.DownloadAssetCommand;
import ru.gravit.launchserver.command.hash.DownloadClientCommand;
import ru.gravit.launchserver.command.hash.IndexAssetCommand;
import ru.gravit.launchserver.command.hash.SyncBinariesCommand;
import ru.gravit.launchserver.command.hash.SyncProfilesCommand;
import ru.gravit.launchserver.command.hash.SyncUpdatesCommand;
import ru.gravit.launchserver.command.hash.UnindexAssetCommand;
import ru.gravit.launchserver.command.modules.LoadModuleCommand;
import ru.gravit.launchserver.command.modules.ModulesCommand;

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

                // Reset string builder
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
    }

    @LauncherAPI
    public abstract void bell() throws IOException;

    @LauncherAPI
    public abstract void clear() throws IOException;

    @LauncherAPI
    public final Map<String, Command> commandsMap() {
        return Collections.unmodifiableMap(commands);
    }

    @LauncherAPI
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

    @LauncherAPI
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

    @LauncherAPI
    public final Command lookup(String name) throws CommandException {
        Command command = commands.get(name);
        if (command == null)
			throw new CommandException(String.format("Unknown command: '%s'", name));
        return command;
    }

    @LauncherAPI
    public abstract String readLine() throws IOException;

    private void readLoop() throws IOException {
        for (String line = readLine(); line != null; line = readLine())
			eval(line, true);
    }

    @LauncherAPI
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
