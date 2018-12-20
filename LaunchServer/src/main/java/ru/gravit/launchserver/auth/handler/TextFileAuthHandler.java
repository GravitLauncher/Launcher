package ru.gravit.launchserver.auth.handler;

import ru.gravit.launcher.serialize.config.TextConfigReader;
import ru.gravit.launcher.serialize.config.TextConfigWriter;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.ConfigEntry;
import ru.gravit.launcher.serialize.config.entry.ConfigEntry.Type;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TextFileAuthHandler extends FileAuthHandler {
    private static StringConfigEntry cc(String value) {
        StringConfigEntry entry = new StringConfigEntry(value, true, 4);
        entry.setComment(0, "\n\t"); // Pre-name
        entry.setComment(2, " "); // Pre-value
        return entry;
    }

    public TextFileAuthHandler(BlockConfigEntry block) {
        super(block);
    }

    @Override
    protected void readAuthFile() throws IOException {
        BlockConfigEntry authFile;
        try (BufferedReader reader = IOHelper.newReader(file)) {
            authFile = TextConfigReader.read(reader, false);
        }

        // Read auths from config block
        Set<Map.Entry<String, ConfigEntry<?>>> entrySet = authFile.getValue().entrySet();
        for (Map.Entry<String, ConfigEntry<?>> entry : entrySet) {
            UUID uuid = UUID.fromString(entry.getKey());
            ConfigEntry<?> value = VerifyHelper.verify(entry.getValue(),
                    v -> v.getType() == Type.BLOCK, "Illegal config entry type: " + uuid);

            // Get auth entry data
            BlockConfigEntry authBlock = (BlockConfigEntry) value;
            String username = authBlock.getEntryValue("username", StringConfigEntry.class);
            String accessToken = authBlock.hasEntry("accessToken") ?
                    authBlock.getEntryValue("accessToken", StringConfigEntry.class) : null;
            String serverID = authBlock.hasEntry("serverID") ?
                    authBlock.getEntryValue("serverID", StringConfigEntry.class) : null;

            // Add auth entry
            addAuth(uuid, new Entry(username, accessToken, serverID));
        }
    }

    @Override
    protected void writeAuthFileTmp() throws IOException {
        boolean next = false;

        // Write auth blocks to map
        Set<Map.Entry<UUID, Entry>> entrySet = entrySet();
        Map<String, ConfigEntry<?>> map = new LinkedHashMap<>(entrySet.size());
        for (Map.Entry<UUID, Entry> entry : entrySet) {
            UUID uuid = entry.getKey();
            Entry auth = entry.getValue();

            // Set auth entry data
            Map<String, ConfigEntry<?>> authMap = new LinkedHashMap<>(entrySet.size());
            authMap.put("username", cc(auth.getUsername()));
            String accessToken = auth.getAccessToken();
            if (accessToken != null)
                authMap.put("accessToken", cc(accessToken));
            String serverID = auth.getServerID();
            if (serverID != null)
                authMap.put("serverID", cc(serverID));

            // Create and add auth block
            BlockConfigEntry authBlock = new BlockConfigEntry(authMap, true, 5);
            if (next)
                authBlock.setComment(0, "\n"); // Pre-name
            else
                next = true;
            authBlock.setComment(2, " "); // Pre-value
            authBlock.setComment(4, "\n"); // Post-comment
            map.put(uuid.toString(), authBlock);
        }

        // Write auth handler file
        try (BufferedWriter writer = IOHelper.newWriter(fileTmp)) {
            BlockConfigEntry authFile = new BlockConfigEntry(map, true, 1);
            authFile.setComment(0, "\n");
            TextConfigWriter.write(authFile, writer, true);
        }
    }
}
