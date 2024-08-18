package pro.gravit.launchserver.auth.profiles;

import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.protect.interfaces.ProfilesProtectHandler;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.ProviderMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class ProfileProvider {
    public static final ProviderMap<ProfileProvider> providers = new ProviderMap<>("ProfileProvider");
    private static boolean registredProviders = false;
    protected transient LaunchServer server;

    public static void registerProviders() {
        if (!registredProviders) {
            providers.register("local", LocalProfileProvider.class);
            registredProviders = true;
        }
    }

    public void init(LaunchServer server) {
        this.server = server;
    }

    public abstract void sync() throws IOException;

    public abstract Set<ClientProfile> getProfiles();

    public abstract void addProfile(ClientProfile profile) throws IOException;

    public abstract void deleteProfile(ClientProfile profile) throws IOException;

    public void close() {

    }

    public ClientProfile getProfile(UUID uuid) {
        for(var e : getProfiles()) {
            if(e.getUUID().equals(uuid)) {
                return e;
            }
        }
        return null;
    }

    public ClientProfile getProfile(String title) {
        for(var e : getProfiles()) {
            if(e.getTitle().equals(title)) {
                return e;
            }
        }
        return null;
    }

    public List<ClientProfile> getProfiles(Client client) {
        List<ClientProfile> profileList;
        Set<ClientProfile> serverProfiles = getProfiles();
        if (server.config.protectHandler instanceof ProfilesProtectHandler protectHandler) {
            profileList = new ArrayList<>(4);
            for (ClientProfile profile : serverProfiles) {
                if (protectHandler.canGetProfile(profile, client)) {
                    profileList.add(profile);
                }
            }
        } else {
            profileList = List.copyOf(serverProfiles);
        }
        return profileList;
    }
}
