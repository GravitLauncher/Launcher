package com.mojang.authlib.yggdrasil;

import java.util.UUID;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.auth.CheckServerRequest;
import ru.gravit.launcher.request.auth.JoinServerRequest;
import ru.gravit.launcher.request.uuid.BatchProfileByUsernameRequest;
import ru.gravit.launcher.request.uuid.ProfileByUUIDRequest;
import ru.gravit.launcher.request.uuid.ProfileByUsernameRequest;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.utils.helper.LogHelper;

// Used to bypass Launcher's class name obfuscation and access API
@SuppressWarnings("unused")
@LauncherAPI
public final class CompatBridge {
    public static final int PROFILES_MAX_BATCH_SIZE = SerializeLimits.MAX_BATCH_SIZE;

    @SuppressWarnings("unused")
    public static CompatProfile checkServer(String username, String serverID) throws Exception {
        LogHelper.debug("CompatBridge.checkServer, Username: '%s', Server ID: %s", username, serverID);
        return CompatProfile.fromPlayerProfile(new CheckServerRequest(username, serverID).request());
    }

    @SuppressWarnings("unused")
    public static boolean joinServer(String username, String accessToken, String serverID) throws Exception {

        // Join server
        LogHelper.debug("LegacyBridge.joinServer, Username: '%s', Access token: %s, Server ID: %s", username, accessToken, serverID);
        return new JoinServerRequest(username, accessToken, serverID).request();
    }

    @SuppressWarnings("unused")
    public static CompatProfile profileByUsername(String username) throws Exception {
        return CompatProfile.fromPlayerProfile(new ProfileByUsernameRequest(username).request());
    }

    @SuppressWarnings("unused")
    public static CompatProfile profileByUUID(UUID uuid) throws Exception {
        return CompatProfile.fromPlayerProfile(new ProfileByUUIDRequest(uuid).request());
    }

    @SuppressWarnings("unused")
    public static CompatProfile[] profilesByUsername(String... usernames) throws Exception {
        PlayerProfile[] profiles = new BatchProfileByUsernameRequest(usernames).request();

        // Convert profiles
        CompatProfile[] resultProfiles = new CompatProfile[profiles.length];
        for (int i = 0; i < profiles.length; i++)
            resultProfiles[i] = CompatProfile.fromPlayerProfile(profiles[i]);

        // We're dones
        return resultProfiles;
    }

    private CompatBridge() {
    }
}
