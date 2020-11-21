package pro.gravit.launchserver.manangers;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.RequiredDAO;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.HookSet;
import pro.gravit.utils.helper.LogHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class SessionManager implements NeedGarbageCollection {

    private final Map<UUID, Entry> clientSet = new ConcurrentHashMap<>(128);
    private final Map<UUID, Set<Entry>> uuidIndex = new ConcurrentHashMap<>(32);
    private final LaunchServer server;
    public HookSet<Client> clientRestoreHook = new HookSet<>();

    public SessionManager(LaunchServer server) {
        this.server = server;
    }


    public boolean addClient(Client client) {
        if(client == null || client.session == null) return false;
        remove(client.session);
        Entry e = new Entry(compressClient(client), client.session);
        clientSet.put(client.session, e);
        if(client.isAuth && client.uuid != null) {
            Set<Entry> uuidSet = uuidIndex.computeIfAbsent(client.uuid, k -> ConcurrentHashMap.newKeySet());
            uuidSet.add(e);
        }
        return true;
    }

    public Stream<UUID> findSessionsByUUID(UUID uuid) {
        Set<Entry> set = uuidIndex.get(uuid);
        if(set != null) return set.stream().map((e) -> e.sessionUuid);
        return null;
    }

    public boolean removeByUUID(UUID uuid) {
        Set<Entry> set = uuidIndex.get(uuid);
        if(set != null) {
            for(Entry e : set) {
                clientSet.remove(e.sessionUuid);
            }
            set.clear();
            uuidIndex.remove(uuid);
        }
        return false;
    }

    public Set<UUID> getSavedUUIDs()
    {
        return uuidIndex.keySet();
    }

    public void clear() {
        clientSet.clear();
        uuidIndex.clear();
    }

    private String compressClient(Client client) {
        return Launcher.gsonManager.gson.toJson(client); //Compress using later
    }

    private Client decompressClient(String client) {
        return Launcher.gsonManager.gson.fromJson(client, Client.class); //Compress using later
    }
    private Client restoreFromString(String data) {
        Client result = decompressClient(data);
        result.updateAuth(server);
        if(result.auth != null && (result.username != null)) {
            if(result.auth.handler instanceof RequiredDAO || result.auth.provider instanceof RequiredDAO || result.auth.textureProvider instanceof RequiredDAO) {
                result.daoObject = server.config.dao.userDAO.findByUsername(result.username);
            }
        }
        if(result.refCount == null) result.refCount = new AtomicInteger(1);
        clientRestoreHook.hook(result);
        return result;
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
            remove(session);
        }
        to_delete.clear();
    }


    public Client getClient(UUID session) {
        Entry e = clientSet.get(session);
        if(e == null) return null;
        return restoreFromString(e.data);
    }


    public Client getOrNewClient(UUID session) {
        Client client = getClient(session);
        return client == null ? new Client(session) : client;
    }

    public boolean remove(UUID session) {
        Entry e =clientSet.remove(session);
        if(e != null) {
            Set<Entry> set = uuidIndex.get(session);
            if(set != null) {
                removeUuidFromIndexSet(set, e, session);
            }
            return true;
        }
        return false;
    }

    private void removeUuidFromIndexSet(Set<Entry> set, Entry e, UUID session) {
        set.remove(e);
        if(set.isEmpty()) {
            uuidIndex.remove(session);
        }
    }
    @Deprecated
    public void removeClient(UUID session) {
        remove(session);
    }


    public void updateClient(UUID session) {
        LogHelper.warning("Using deprecated method: sessionManager.updateClient");
    }

    public Set<Client> getSessions() {
        // TODO: removeme
        LogHelper.warning("Using deprecated method: sessionManager.getSession");
        return new HashSet<>();
    }

    public void loadSessions(Set<Client> set) {
        LogHelper.warning("Using deprecated method: sessionManager.loadSessions");
        //clientSet.putAll(set.stream().collect(Collectors.toMap(c -> c.session, Function.identity())));
    }
    private static class Entry {
        public String data;
        public UUID sessionUuid;
        public long timestamp;

        public Entry(String data, UUID sessionUuid) {
            this.data = data;
            this.sessionUuid = sessionUuid;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
