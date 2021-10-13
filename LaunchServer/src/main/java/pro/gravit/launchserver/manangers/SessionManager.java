package pro.gravit.launchserver.manangers;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.HookSet;
import pro.gravit.utils.helper.IOHelper;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class SessionManager implements NeedGarbageCollection {

    private final LaunchServer server;
    public HookSet<Client> clientRestoreHook = new HookSet<>();

    public SessionManager(LaunchServer server) {
        this.server = server;
    }


    public boolean addClient(Client client) {
        if (client == null || client.session == null) return false;
        return server.config.sessions.writeSession(client.uuid, client.session, compressClient(client));
    }

    public Stream<UUID> findSessionsByUUID(UUID uuid) {
        return server.config.sessions.getSessionsFromUserUUID(uuid);
    }

    public boolean removeByUUID(UUID uuid) {
        return server.config.sessions.deleteSessionsByUserUUID(uuid);
    }

    public void clear() {
        server.config.sessions.clear();
    }

    private byte[] compressClient(Client client) {
        return IOHelper.encode(Launcher.gsonManager.gson.toJson(client)); //Compress using later
    }

    private Client decompressClient(byte[] client) {
        return Launcher.gsonManager.gson.fromJson(IOHelper.decode(client), Client.class); //Compress using later
    }

    private Client restoreFromString(byte[] data) {
        Client result = decompressClient(data);
        result.updateAuth(server);
        if (result.auth != null && (result.username != null)) {
            result.coreObject = result.auth.core.getUserByUUID(result.uuid);
        }
        if (result.refCount == null) result.refCount = new AtomicInteger(1);
        clientRestoreHook.hook(result);
        return result;
    }

    @Override
    public void garbageCollection() {
    }


    public Client getClient(UUID session) {
        if (session == null) return null;
        byte[] data = server.config.sessions.getSessionData(session);
        if (data == null) return null;
        return restoreFromString(data);
    }


    public Client getOrNewClient(UUID session) {
        Client client = getClient(session);
        return client == null ? new Client(session) : client;
    }

    public boolean remove(UUID session) {
        return server.config.sessions.deleteSession(session);
    }
}
