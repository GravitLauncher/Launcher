package pro.gravit.launcher.base.profiles;

import pro.gravit.launcher.core.api.model.User;
import pro.gravit.utils.helper.IOHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PlayerProfile implements User {

    public final UUID uuid;
    public final String username;
    public final Map<String, Texture> assets;
    public final Map<String, String> properties;


    @Deprecated
    public PlayerProfile(UUID uuid, String username, Texture skin, Texture cloak) {
        this(uuid, username, skin, cloak, new HashMap<>());
    }

    @Deprecated
    public PlayerProfile(UUID uuid, String username, Texture skin, Texture cloak, Map<String, String> properties) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.username = username;
        this.assets = new HashMap<>();
        if (skin != null) {
            this.assets.put("SKIN", skin);
        }
        if (cloak != null) {
            this.assets.put("CAPE", cloak);
        }
        this.properties = properties;
    }

    public PlayerProfile(UUID uuid, String username, Map<String, Texture> assets, Map<String, String> properties) {
        this.uuid = uuid;
        this.username = username;
        this.assets = assets;
        this.properties = properties;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public PlayerProfile(User user) {
        this.uuid = user.getUUID();
        this.username = user.getUsername();
        this.assets = new HashMap<>((Map) user.getAssets());
        this.properties = user.getProperties();
    }

    public static PlayerProfile newOfflineProfile(String username) {
        return new PlayerProfile(offlineUUID(username), username, new HashMap<>(), new HashMap<>());
    }

    public static UUID offlineUUID(String username) {
        return UUID.nameUUIDFromBytes(IOHelper.encodeASCII("OfflinePlayer:" + username));
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Map<String, pro.gravit.launcher.core.api.model.Texture> getAssets() {
        return (Map) assets;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }
}
