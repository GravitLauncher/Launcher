package pro.gravit.launchserver.auth.session;

import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launchserver.manangers.SessionManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class MemorySessionStorage extends SessionStorage implements NeedGarbageCollection {

    private final Map<UUID, Entry> clientSet = new ConcurrentHashMap<>(128);
    private final Map<UUID, Set<Entry>> uuidIndex = new ConcurrentHashMap<>(32);

    @Override
    public byte[] getSessionData(UUID session) {

        Entry e = clientSet.get(session);
        if(e == null) return null;
        return e.data;
    }

    @Override
    public Stream<UUID> getSessionsFromUserUUID(UUID userUUID) {
        Set<Entry> set = uuidIndex.get(userUUID);
        if(set != null) return set.stream().map((e) -> e.sessionUuid);
        return null;
    }

    @Override
    public boolean writeSession(UUID userUUID, UUID sessionUUID, byte[] data) {
        deleteSession(sessionUUID);
        Entry e = new Entry(data, sessionUUID);
        clientSet.put(sessionUUID, e);
        if(userUUID != null) {
            Set<Entry> uuidSet = uuidIndex.computeIfAbsent(userUUID, k -> ConcurrentHashMap.newKeySet());
            uuidSet.add(e);
        }
        return false;
    }

    @Override
    public boolean deleteSession(UUID sessionUUID) {
        Entry e =clientSet.remove(sessionUUID);
        if(e != null) {
            Set<Entry> set = uuidIndex.get(sessionUUID);
            if(set != null) {
                removeUuidFromIndexSet(set, e, sessionUUID);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteSessionsByUserUUID(UUID userUUID) {
        Set<Entry> set = uuidIndex.get(userUUID);
        if(set != null) {
            for(Entry e : set) {
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

    private void removeUuidFromIndexSet(Set<Entry> set, Entry e, UUID session) {
        set.remove(e);
        if(set.isEmpty()) {
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
            if(timestamp + session_timeout < time)
                to_delete.add(uuid);
        });
        for(UUID session : to_delete) {
            deleteSession(session);
        }
        to_delete.clear();
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
}
