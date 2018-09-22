package ru.gravit.launchserver.manangers;

import java.util.HashSet;
import java.util.Set;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.NeedGarbageCollection;
import ru.gravit.launchserver.socket.Client;

public class SessionManager implements NeedGarbageCollection {
    @LauncherAPI
    public static final long SESSION_TIMEOUT = 10 * 60 * 1000; // 10 минут
    private Set<Client> clientSet = new HashSet<>(128);

    @LauncherAPI
    public boolean addClient(Client client) {
        clientSet.add(client);
        return true;
    }

    @Override
    @LauncherAPI
    public void garbageCollection() {
        long time = System.currentTimeMillis();
        clientSet.removeIf(c -> c.timestamp + SESSION_TIMEOUT < time);
    }

    @LauncherAPI
    public Client getClient(long session) {
        for (Client c : clientSet)
            if (c.session == session) return c;
        return null;
    }

    @LauncherAPI
    public Client getOrNewClient(long session) {
        for (Client c : clientSet)
            if (c.session == session) return c;
        Client newClient = new Client(session);
        clientSet.add(newClient);
        return newClient;
    }

    @LauncherAPI
    public void updateClient(long session) {
        for (Client c : clientSet)
            if (c.session == session) {
                c.up();
                return;
            }
    }
}
