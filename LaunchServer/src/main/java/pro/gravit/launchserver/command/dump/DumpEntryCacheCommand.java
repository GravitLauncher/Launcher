package pro.gravit.launchserver.command.dump;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import pro.gravit.launcher.Launcher;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.handler.CachedAuthHandler;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class DumpEntryCacheCommand extends Command {
    public DumpEntryCacheCommand(LaunchServer server) {
        super(server);
        childCommands.put("load", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 2);
                AuthProviderPair pair = server.config.getAuthProviderPair(args[0]);
                if (pair == null) throw new IllegalStateException(String.format("Auth %s not found", args[0]));
                if (!(pair.handler instanceof CachedAuthHandler))
                    throw new UnsupportedOperationException("This command used only CachedAuthHandler");
                CachedAuthHandler authHandler = (CachedAuthHandler) pair.handler;

                LogHelper.info("CachedAuthHandler read from %s", args[0]);
                int size_entry;
                int size_username;
                try (Reader reader = IOHelper.newReader(Paths.get(args[1]))) {
                    EntryAndUsername entryAndUsername = Launcher.gsonManager.configGson.fromJson(reader, EntryAndUsername.class);
                    size_entry = entryAndUsername.entryCache.size();
                    size_username = entryAndUsername.usernameCache.size();
                    authHandler.loadEntryCache(entryAndUsername.entryCache);
                    authHandler.loadUsernameCache(entryAndUsername.usernameCache);

                }
                LogHelper.subInfo("Readed %d entryCache %d usernameCache", size_entry, size_username);
            }
        });
        childCommands.put("unload", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 2);
                AuthProviderPair pair = server.config.getAuthProviderPair(args[0]);
                if (pair == null) throw new IllegalStateException(String.format("Auth %s not found", args[0]));
                if (!(pair.handler instanceof CachedAuthHandler))
                    throw new UnsupportedOperationException("This command used only CachedAuthHandler");
                CachedAuthHandler authHandler = (CachedAuthHandler) pair.handler;

                LogHelper.info("CachedAuthHandler write to %s", args[1]);
                Map<UUID, CachedAuthHandler.Entry> entryCache = authHandler.getEntryCache();
                Map<String, UUID> usernamesCache = authHandler.getUsernamesCache();
                EntryAndUsername serializable = new EntryAndUsername();
                serializable.entryCache = entryCache;
                serializable.usernameCache = usernamesCache;
                try (Writer writer = IOHelper.newWriter(Paths.get(args[1]))) {
                    Launcher.gsonManager.configGson.toJson(serializable, writer);
                }
                LogHelper.subInfo("Write %d entryCache, %d usernameCache", entryCache.size(), usernamesCache.size());
            }
        });
    }

    @Override
    public String getArgsDescription() {
        return "[load/unload] [auth_id] [filename]";
    }

    @Override
    public String getUsageDescription() {
        return "Load or unload AuthHandler Entry cache";
    }

    @Override
    public void invoke(String... args) throws Exception {
        invokeSubcommands(args);
    }

    public class EntryAndUsername {
        public Map<UUID, CachedAuthHandler.Entry> entryCache;
        public Map<String, UUID> usernameCache;
    }
}
