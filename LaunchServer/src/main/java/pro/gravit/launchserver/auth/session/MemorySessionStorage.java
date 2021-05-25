package pro.gravit.launchserver.auth.session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.IOHelper;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class MemorySessionStorage extends SessionStorage implements NeedGarbageCollection, AutoCloseable {

    private transient final Map<UUID, Entry> clientSet = new ConcurrentHashMap<>(128);
    private transient final Map<UUID, Set<Entry>> uuidIndex = new ConcurrentHashMap<>(32);
    private transient final Logger logger = LogManager.getLogger();
    public boolean autoDump = false;
    public String dumpFile = "sessions.json";

    @Override
    public void init(LaunchServer server) {
        super.init(server);
        if (autoDump) {
            loadSessionsData();
            garbageCollection();
        }
    }

    @Override
    public byte[] getSessionData(UUID session) {

        Entry e = clientSet.get(session);
        if (e == null) return null;
        return e.data;
    }

    @Override
    public Stream<UUID> getSessionsFromUserUUID(UUID userUUID) {
        Set<Entry> set = uuidIndex.get(userUUID);
        if (set != null) return set.stream().map((e) -> e.sessionUuid);
        return null;
    }

    @Override
    public boolean writeSession(UUID userUUID, UUID sessionUUID, byte[] data) {
        deleteSession(sessionUUID);
        Entry e = new Entry(data, sessionUUID);
        clientSet.put(sessionUUID, e);
        if (userUUID != null) {
            Set<Entry> uuidSet = uuidIndex.computeIfAbsent(userUUID, k -> ConcurrentHashMap.newKeySet());
            uuidSet.add(e);
        }
        return false;
    }

    @Override
    public boolean deleteSession(UUID sessionUUID) {
        Entry e = clientSet.remove(sessionUUID);
        if (e != null) {
            Set<Entry> set = uuidIndex.get(sessionUUID);
            if (set != null) {
                removeUuidFromIndexSet(set, e, sessionUUID);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteSessionsByUserUUID(UUID userUUID) {
        Set<Entry> set = uuidIndex.get(userUUID);
        if (set != null) {
            for (Entry e : set) {
                clientSet.remove(e.sessionUuid);
            }
            set.clear();
            uuidIndex.remove(userUUID);
        }
        return true;
    }

    @Override
    public void clear() {
        clientSet.clear();
        uuidIndex.clear();
    }

    public void dumpSessionsData() {
        DumpedData dumpedData = new DumpedData(clientSet, uuidIndex);
        Path path = Paths.get(dumpFile);
        try (Writer writer = IOHelper.newWriter(path)) {
            Launcher.gsonManager.gson.toJson(dumpedData, writer);
        } catch (Throwable e) {
            logger.error("Sessions can't be saved", e);
        }
    }

    public void loadSessionsData() {
        Path path = Paths.get(dumpFile);
        if (!Files.exists(path)) return;
        try (Reader reader = IOHelper.newReader(path)) {
            DumpedData data = Launcher.gsonManager.gson.fromJson(reader, DumpedData.class);
            clientSet.putAll(data.clientSet);
            uuidIndex.putAll(data.uuidIndex);
        } catch (Throwable e) {
            logger.error("Sessions can't be loaded", e);
        }
    }

    @Override
    public void lockSession(UUID sessionUUID) {

    }

    @Override
    public void lockUser(UUID userUUID) {

    }

    @Override
    public void unlockSession(UUID sessionUUID) {

    }

    @Override
    public void unlockUser(UUID userUUID) {

    }

    private void removeUuidFromIndexSet(Set<Entry> set, Entry e, UUID session) {
        set.remove(e);
        if (set.isEmpty()) {
            uuidIndex.remove(session);
        }
    }

    @Override
    public void garbageCollection() {
        long time = System.currentTimeMillis();
        long session_timeout = server.config.netty.performance.sessionLifetimeMs;
        Set<UUID> to_delete = new HashSet<>(32);
        clientSet.forEach((uuid, entry) -> {
            long timestamp = entry.timestamp;
            if (timestamp + session_timeout < time)
                to_delete.add(uuid);
        });
        for (UUID session : to_delete) {
            deleteSession(session);
        }
        to_delete.clear();
    }

    @Override
    public void close() {
        if (autoDump) {
            garbageCollection();
            dumpSessionsData();
        }
    }

    private static class Entry {
        public byte[] data;
        public UUID sessionUuid;
        public long timestamp;

        public Entry(byte[] data, UUID sessionUuid) {
            this.data = data;
            this.sessionUuid = sessionUuid;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static class DumpedData {
        private final Map<UUID, Entry> clientSet;
        private final Map<UUID, Set<Entry>> uuidIndex;

        private DumpedData(Map<UUID, Entry> clientSet, Map<UUID, Set<Entry>> uuidIndex) {
            this.clientSet = clientSet;
            this.uuidIndex = uuidIndex;
        }
    }
}
