package pro.gravit.launchserver.auth.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.launchserver.auth.provider.AuthProviderResult;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public abstract class CachedAuthHandler extends AuthHandler implements NeedGarbageCollection, Reconfigurable {
    private transient final Map<UUID, Entry> entryCache = new HashMap<>(1024);
    private transient final Map<String, UUID> usernamesCache = new HashMap<>(1024);
    private transient final Logger logger = LogManager.getLogger();

    @Override
    public Map<String, Command> getCommands() {
        Map<String, Command> commands = new HashMap<>();
        commands.put("clear", new SubCommand() {
            @Override
            public void invoke(String... args) {
                long entryCacheSize = entryCache.size();
                long usernamesCacheSize = usernamesCache.size();
                entryCache.clear();
                usernamesCache.clear();
                logger.info("Cleared cache: {} Entry {} Usernames", entryCacheSize, usernamesCacheSize);
            }
        });
        commands.put("load", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 2);

                logger.info("CachedAuthHandler read from {}", args[0]);
                int size_entry;
                int size_username;
                try (Reader reader = IOHelper.newReader(Paths.get(args[1]))) {
                    EntryAndUsername entryAndUsername = Launcher.gsonManager.configGson.fromJson(reader, EntryAndUsername.class);
                    size_entry = entryAndUsername.entryCache.size();
                    size_username = entryAndUsername.usernameCache.size();
                    loadEntryCache(entryAndUsername.entryCache);
                    loadUsernameCache(entryAndUsername.usernameCache);

                }
                logger.info("Read {} entryCache {} usernameCache", size_entry, size_username);
            }
        });
        commands.put("unload", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 2);

                logger.info("CachedAuthHandler write to {}", args[1]);
                Map<UUID, CachedAuthHandler.Entry> entryCache = getEntryCache();
                Map<String, UUID> usernamesCache = getUsernamesCache();
                EntryAndUsername serializable = new EntryAndUsername();
                serializable.entryCache = entryCache;
                serializable.usernameCache = usernamesCache;
                try (Writer writer = IOHelper.newWriter(Paths.get(args[1]))) {
                    Launcher.gsonManager.configGson.toJson(serializable, writer);
                }
                logger.info("Write {} entryCache, {} usernameCache", entryCache.size(), usernamesCache.size());
            }
        });
        return commands;
    }

    protected void addEntry(Entry entry) {
        Entry previous = entryCache.put(entry.uuid, entry);
        if (previous != null)
            usernamesCache.remove(CommonHelper.low(previous.username));
        usernamesCache.put(CommonHelper.low(entry.username), entry.uuid);
    }

    @Override
    public final synchronized UUID auth(AuthProviderResult result) throws IOException {
        Entry entry = getEntry(result.username);
        if (entry == null || !updateAuth(entry.uuid, entry.username, result.accessToken))
            return authError(String.format("UUID is null for username '%s'", result.username));

        // Update cached access token (and username case)
        entry.username = result.username;
        entry.accessToken = result.accessToken;
        entry.serverID = null;
        return entry.uuid;
    }

    @Override
    public synchronized UUID checkServer(String username, String serverID) throws IOException {
        Entry entry = getEntry(username);
        return entry != null && username.equals(entry.username) &&
                serverID.equals(entry.serverID) ? entry.uuid : null;
    }

    protected abstract Entry fetchEntry(String username) throws IOException;

    protected abstract Entry fetchEntry(UUID uuid) throws IOException;

    private Entry getEntry(String username) throws IOException {
        UUID uuid = usernamesCache.get(CommonHelper.low(username));
        if (uuid != null)
            return getEntry(uuid);

        // Fetch entry by username
        Entry entry = fetchEntry(username);
        if (entry != null)
            addEntry(entry);

        // Return what we got
        return entry;
    }

    private Entry getEntry(UUID uuid) throws IOException {
        Entry entry = entryCache.get(uuid);
        if (entry == null) {
            entry = fetchEntry(uuid);
            if (entry != null)
                addEntry(entry);
        }
        return entry;
    }

    @Override
    public synchronized boolean joinServer(String username, String accessToken, String serverID) throws IOException {
        Entry entry = getEntry(username);
        if (entry == null || !username.equals(entry.username) || !accessToken.equals(entry.accessToken) ||
                !updateServerID(entry.uuid, serverID))
            return false; // Account doesn't exist or invalid access token

        // Update cached server ID
        entry.serverID = serverID;
        return true;
    }

    public synchronized void garbageCollection() {
        entryCache.clear();
        usernamesCache.clear();
    }

    public Map<UUID, Entry> getEntryCache() {
        return entryCache;
    }

    public Map<String, UUID> getUsernamesCache() {
        return usernamesCache;
    }

    public void loadEntryCache(Map<UUID, Entry> map) {
        entryCache.putAll(map);
    }

    public void loadUsernameCache(Map<String, UUID> map) {
        usernamesCache.putAll(map);
    }

    protected abstract boolean updateAuth(UUID uuid, String username, String accessToken) throws IOException;

    protected abstract boolean updateServerID(UUID uuid, String serverID) throws IOException;

    @Override
    public final synchronized UUID usernameToUUID(String username) throws IOException {
        Entry entry = getEntry(username);
        return entry == null ? null : entry.uuid;
    }

    @Override
    public final synchronized String uuidToUsername(UUID uuid) throws IOException {
        Entry entry = getEntry(uuid);
        return entry == null ? null : entry.username;
    }

    public static final class Entry {

        public final UUID uuid;
        private String username;
        private String accessToken;
        private String serverID;


        public Entry(UUID uuid, String username, String accessToken, String serverID) {
            this.uuid = Objects.requireNonNull(uuid, "uuid");
            this.username = Objects.requireNonNull(username, "username");
            this.accessToken = accessToken == null ? null : SecurityHelper.verifyToken(accessToken);
            this.serverID = serverID == null ? null : VerifyHelper.verifyServerID(serverID);
        }
    }

    protected static class EntryAndUsername {
        public Map<UUID, CachedAuthHandler.Entry> entryCache;
        public Map<String, UUID> usernameCache;
    }
}
