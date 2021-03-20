package pro.gravit.launchserver.auth.session;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.ProviderMap;

import java.util.UUID;
import java.util.stream.Stream;

public abstract class SessionStorage {
    public static ProviderMap<SessionStorage> providers = new ProviderMap<>();
    private static boolean registeredProviders = false;
    protected transient LaunchServer server;

    public static void registerProviders() {
        if (!registeredProviders) {
            providers.register("memory", MemorySessionStorage.class);
            registeredProviders = true;
        }
    }

    public abstract byte[] getSessionData(UUID session);

    public abstract Stream<UUID> getSessionsFromUserUUID(UUID userUUID);

    public abstract boolean writeSession(UUID userUUID, UUID sessionUUID, byte[] data);

    public abstract boolean deleteSession(UUID sessionUUID);

    public boolean deleteSessionsByUserUUID(UUID userUUID) {
        getSessionsFromUserUUID(userUUID).forEach(this::deleteSession);
        return true;
    }

    public abstract void clear();

    public abstract void lockSession(UUID sessionUUID);

    public abstract void lockUser(UUID userUUID);

    public abstract void unlockSession(UUID sessionUUID);

    public abstract void unlockUser(UUID userUUID);

    public void init(LaunchServer server) {
        this.server = server;
    }
}
