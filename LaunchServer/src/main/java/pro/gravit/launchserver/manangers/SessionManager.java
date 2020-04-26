package pro.gravit.launchserver.manangers;

import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launchserver.socket.Client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SessionManager implements NeedGarbageCollection {

    public static final long SESSION_TIMEOUT = 3 * 60 * 60 * 1000; // 3 часа
    private final Map<Long, Client> clientSet = new HashMap<>(128);


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


    public Client getClient(long session) {
        return clientSet.get(session);
    }


    public Client getOrNewClient(long session) {
        return clientSet.computeIfAbsent(session, Client::new);
    }

    public Client removeClient(long session) {
        return clientSet.remove(session);
    }


    public void updateClient(long session) {
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
