package pro.gravit.launchserver.manangers;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.RequiredDAO;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.helper.LogHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SessionManager implements NeedGarbageCollection {

    public static final long SESSION_TIMEOUT = 3 * 60 * 60 * 1000; // 3 часа
    private final Map<UUID, Entry> clientSet = new ConcurrentHashMap<>(128);
    private final LaunchServer server;

    public SessionManager(LaunchServer server) {
        this.server = server;
    }


    public boolean addClient(Client client) {
        if(client == null) return false;
        clientSet.put(client.session, new Entry(compressClient(client)));
        return true;
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

        return result;
    }

    @Override
    public void garbageCollection() {
        long time = System.currentTimeMillis();
        clientSet.entrySet().removeIf(entry -> {
            long timestamp = entry.getValue().timestamp;
            return (timestamp + SESSION_TIMEOUT < time);
        });
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

    public void removeClient(UUID session) {
        clientSet.remove(session);
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
        public long timestamp;

        public Entry(String data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
