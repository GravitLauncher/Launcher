package pro.gravit.launchserver.manangers;

import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launchserver.socket.Client;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SessionManager implements NeedGarbageCollection {

    public static final long SESSION_TIMEOUT = 3 * 60 * 60 * 1000; // 3 часа
    private final Map<UUID, Client> clientSet = new HashMap<>(128);


    public boolean addClient(Client client) {
        clientSet.put(client.session, client);
        return true;
    }

    @Override
    public void garbageCollection() {
        long time = System.currentTimeMillis();
        clientSet.entrySet().removeIf(entry -> {
            Client c = entry.getValue();
            return (c.timestamp + SESSION_TIMEOUT < time);
        });
    }


    public Client getClient(UUID session) {
        return clientSet.get(session);
    }


    public Client getOrNewClient(UUID session) {
        return clientSet.computeIfAbsent(session, Client::new);
    }

    public Client removeClient(UUID session) {
        return clientSet.remove(session);
    }


    public void updateClient(UUID session) {
        Client c = clientSet.get(session);
        if (c != null) {
            c.up();
            return;
        }
        Client newClient = new Client(session);
        clientSet.put(session, newClient);
    }

    public Set<Client> getSessions() {
        // TODO: removeme
        return new HashSet<>(clientSet.values());
    }

    public void loadSessions(Set<Client> set) {
        clientSet.putAll(set.stream().collect(Collectors.toMap(c -> c.session, Function.identity())));
    }
}
