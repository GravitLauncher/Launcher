package ru.gravit.launcher.profiles;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.stream.StreamObject;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.VerifyHelper;

public final class PlayerProfile extends StreamObject {
    @LauncherAPI
    public static PlayerProfile newOfflineProfile(String username) {
        return new PlayerProfile(offlineUUID(username), username, null, null);
    }

    @LauncherAPI
    public static UUID offlineUUID(String username) {
        return UUID.nameUUIDFromBytes(IOHelper.encodeASCII("OfflinePlayer:" + username));
    }

    @LauncherAPI
    public final UUID uuid;

    @LauncherAPI
    public final String username;

    @LauncherAPI
    public final Texture skin, cloak;

    @LauncherAPI
    public PlayerProfile(HInput input) throws IOException {
        uuid = input.readUUID();
        username = VerifyHelper.verifyUsername(input.readString(64));
        skin = input.readBoolean() ? new Texture(input) : null;
        cloak = input.readBoolean() ? new Texture(input) : null;
    }

    @LauncherAPI
    public PlayerProfile(UUID uuid, String username, Texture skin, Texture cloak) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.username = VerifyHelper.verifyUsername(username);
        this.skin = skin;
        this.cloak = cloak;
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
