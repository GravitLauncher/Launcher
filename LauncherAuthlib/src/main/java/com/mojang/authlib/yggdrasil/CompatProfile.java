package com.mojang.authlib.yggdrasil;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.utils.helper.SecurityHelper;

import java.util.UUID;


public class CompatProfile {
    public static final String SKIN_URL_PROPERTY = Launcher.SKIN_URL_PROPERTY;
    public static final String SKIN_DIGEST_PROPERTY = Launcher.SKIN_DIGEST_PROPERTY;
    public static final String CLOAK_URL_PROPERTY = Launcher.CLOAK_URL_PROPERTY;
    public static final String CLOAK_DIGEST_PROPERTY = Launcher.CLOAK_DIGEST_PROPERTY;
    // Instance
    public final UUID uuid;
    public final String uuidHash, username;
    public final String skinURL, skinDigest;
    public final String cloakURL, cloakDigest;

    public CompatProfile(UUID uuid, String username, String skinURL, String skinDigest, String cloakURL, String cloakDigest) {
        this.uuid = uuid;
        uuidHash = Launcher.toHash(uuid);
        this.username = username;
        this.skinURL = skinURL;
        this.skinDigest = skinDigest;
        this.cloakURL = cloakURL;
        this.cloakDigest = cloakDigest;
    }

    public static CompatProfile fromPlayerProfile(PlayerProfile profile) {
        return profile == null ? null : new CompatProfile(profile.uuid, profile.username,
                profile.skin == null ? null : profile.skin.url,
                profile.skin == null ? null : SecurityHelper.toHex(profile.skin.digest),
                profile.cloak == null ? null : profile.cloak.url,
                profile.cloak == null ? null : SecurityHelper.toHex(profile.cloak.digest)
        );
    }

    public int countProperties() {
        int count = 0;
        if (skinURL != null)
            count++;
        if (skinDigest != null)
            count++;
        if (cloakURL != null)
            count++;
        if (cloakDigest != null)
            count++;
        return count;
    }
}
