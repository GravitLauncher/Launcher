package pro.gravit.launcher.profiles;

import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launcher.serialize.stream.StreamObject;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public final class PlayerProfile extends StreamObject {

    public final UUID uuid;
    public final String username;
    public final Texture skin, cloak;


    public PlayerProfile(HInput input) throws IOException {
        uuid = input.readUUID();
        username = VerifyHelper.verifyUsername(input.readString(64));
        skin = input.readBoolean() ? new Texture(input) : null;
        cloak = input.readBoolean() ? new Texture(input) : null;
    }


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

    @Override
    public void write(HOutput output) throws IOException {
        output.writeUUID(uuid);
        output.writeString(username, 64);

        // Write textures
        output.writeBoolean(skin != null);
        if (skin != null)
            skin.write(output);
        output.writeBoolean(cloak != null);
        if (cloak != null)
            cloak.write(output);
    }

}
