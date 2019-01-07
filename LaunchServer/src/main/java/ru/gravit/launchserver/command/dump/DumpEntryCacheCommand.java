package ru.gravit.launchserver.command.dump;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.handler.CachedAuthHandler;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.logging.LogHelper;

public class DumpEntryCacheCommand extends Command {
    public DumpEntryCacheCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[load/unload] [filename]";
    }

    @Override
    public String getUsageDescription() {
        return "Load or unload AuthHandler Entry cache";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args,2);
        if(!(server.config.authHandler instanceof CachedAuthHandler)) throw new UnsupportedOperationException("This command used only CachedAuthHandler");
        CachedAuthHandler authHandler = (CachedAuthHandler) server.config.authHandler;
        if(args[0].equals("unload"))
        {
            LogHelper.info("CachedAuthHandler write to %s",args[1]);
            Map<UUID,CachedAuthHandler.Entry> entryCache = authHandler.getEntryCache();
            Map<String, UUID> usernamesCache = authHandler.getUsernamesCache();
            EntryAndUsername serializable = new EntryAndUsername();
            serializable.entryCache = entryCache;
            serializable.usernameCache = usernamesCache;
            try(Writer writer = IOHelper.newWriter(Paths.get(args[1])))
            {
                LaunchServer.gson.toJson(serializable,writer);
            }
            LogHelper.subInfo("Write %d entryCache, %d usernameCache",entryCache.size(),usernamesCache.size());
        } else if(args[0].equals("load"))
        {
            LogHelper.info("CachedAuthHandler read from %s",args[1]);
            int size_entry = 0;
            int size_username = 0;
            try(Reader reader = IOHelper.newReader(Paths.get(args[1])))
            {
                EntryAndUsername entryAndUsername = LaunchServer.gson.fromJson(reader,EntryAndUsername.class);
                size_entry = entryAndUsername.entryCache.size();
                size_username = entryAndUsername.usernameCache.size();
                authHandler.loadEntryCache(entryAndUsername.entryCache);
                authHandler.loadUsernameCache(entryAndUsername.usernameCache);

            }
            LogHelper.subInfo("Readed %d entryCache %d usernameCache",size_entry,size_username);
        }
    }

    public class EntryAndUsername
    {
        public Map<UUID,CachedAuthHandler.Entry> entryCache;
        public Map<String, UUID> usernameCache;
    }
}
