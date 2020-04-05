package com.mojang.authlib.yggdrasil;

import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launcher.request.auth.CheckServerRequest;
import pro.gravit.launcher.request.auth.JoinServerRequest;
import pro.gravit.launcher.request.uuid.BatchProfileByUsernameRequest;
import pro.gravit.launcher.request.uuid.ProfileByUUIDRequest;
import pro.gravit.launcher.request.uuid.ProfileByUsernameRequest;
import pro.gravit.utils.helper.LogHelper;

import java.util.UUID;

// Used to bypass Launcher's class name obfuscation and access API

public class CompatBridge {
    public static final int PROFILES_MAX_BATCH_SIZE = 128;

    private CompatBridge() {
    }

    public static CompatProfile checkServer(String username, String serverID) throws Exception {
        LogHelper.debug("CompatBridge.checkServer, Username: '%s', Server ID: %s", username, serverID);
        return CompatProfile.fromPlayerProfile(new CheckServerRequest(username, serverID).request().playerProfile);
    }

    public static boolean joinServer(String username, String accessToken, String serverID) throws Exception {

        // Join server
        LogHelper.debug("LegacyBridge.joinServer, Username: '%s', Access token: %s, Server ID: %s", username, accessToken, serverID);
        return new JoinServerRequest(username, accessToken, serverID).request().allow;
    }

    public static CompatProfile profileByUsername(String username) throws Exception {
        return CompatProfile.fromPlayerProfile(new ProfileByUsernameRequest(username).request().playerProfile);
    }

    public static CompatProfile profileByUUID(UUID uuid) throws Exception {
        return CompatProfile.fromPlayerProfile(new ProfileByUUIDRequest(uuid).request().playerProfile);
    }

    public static CompatProfile[] profilesByUsername(String... usernames) throws Exception {
        PlayerProfile[] profiles = new BatchProfileByUsernameRequest(usernames).request().playerProfiles;

        // Convert profiles
        CompatProfile[] resultProfiles = new CompatProfile[profiles.length];
        for (int i = 0; i < profiles.length; i++)
            resultProfiles[i] = CompatProfile.fromPlayerProfile(profiles[i]);

        // We're dones
        return resultProfiles;
    }
}
