package pro.gravit.launcher.profiles;

import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.VerifyHelper;

import java.util.Objects;
import java.util.UUID;

public final class PlayerProfile {

    public final UUID uuid;
    public final String username;
    public final Texture skin, cloak;


    public PlayerProfile(UUID uuid, String username, Texture skin, Texture cloak) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.username = VerifyHelper.verifyUsername(username);
        this.skin = skin;
        this.cloak = cloak;
    }

    public static PlayerProfile newOfflineProfile(String username) {
        return new PlayerProfile(offlineUUID(username), username, null, null);
    }

    public static UUID offlineUUID(String username) {
        return UUID.nameUUIDFromBytes(IOHelper.encodeASCII("OfflinePlayer:" + username));
    }

}
