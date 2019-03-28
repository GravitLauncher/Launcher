package ru.gravit.launchserver.manangers;

import ru.gravit.launcher.NeedGarbageCollection;
import ru.gravit.launchserver.socket.Client;

import java.util.HashSet;
import java.util.Set;

public class SessionManager implements NeedGarbageCollection {

    public static final long SESSION_TIMEOUT = 3 * 60 * 60 * 1000; // 3 часа
    public static final boolean GARBAGE_SERVER = Boolean.parseBoolean(System.getProperty("launcher.garbageSessionsServer", "false"));
    private HashSet<Client> clientSet = new HashSet<>(128);


    public boolean addClient(Client client) {
        clientSet.add(client);
        return true;
    }

    @Override
    public void garbageCollection() {
        long time = System.currentTimeMillis();
        clientSet.removeIf(c -> (c.timestamp + SESSION_TIMEOUT < time) && ((c.type == Client.Type.USER) || ((c.type == Client.Type.SERVER) && GARBAGE_SERVER)));
    }


    public Client getClient(long session) {
        for (Client c : clientSet)
            if (c.session == session) return c;
        return null;
    }


    public Client getOrNewClient(long session) {
        for (Client c : clientSet)
            if (c.session == session) return c;
        Client newClient = new Client(session);
        clientSet.add(newClient);
        return newClient;
    }


    public void updateClient(long session) {
        for (Client c : clientSet) {
            if (c.session == session) {
                c.up();
                return;
            }
        }
        Client newClient = new Client(session);
        clientSet.add(newClient);
    }

    public Set<Client> getSessions() {
        return clientSet;
    }

    public void loadSessions(Set<Client> set) {
        clientSet.addAll(set);
    }
}
