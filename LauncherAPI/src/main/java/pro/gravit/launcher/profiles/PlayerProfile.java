package pro.gravit.launcher.profiles;

import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.VerifyHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PlayerProfile {

    public final UUID uuid;
    public final String username;
    public final Texture skin, cloak;
    public final Map<String, String> properties;


    public PlayerProfile(UUID uuid, String username, Texture skin, Texture cloak) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.username = username;
        this.skin = skin;
        this.cloak = cloak;
        this.properties = new HashMap<>();
    }

    public PlayerProfile(UUID uuid, String username, Texture skin, Texture cloak, Map<String, String> properties) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.username = username;
        this.skin = skin;
        this.cloak = cloak;
        this.properties = properties;
    }

    public static PlayerProfile newOfflineProfile(String username) {
        return new PlayerProfile(offlineUUID(username), username, null, null);
    }

    public static UUID offlineUUID(String username) {
        return UUID.nameUUIDFromBytes(IOHelper.encodeASCII("OfflinePlayer:" + username));
    }

}
