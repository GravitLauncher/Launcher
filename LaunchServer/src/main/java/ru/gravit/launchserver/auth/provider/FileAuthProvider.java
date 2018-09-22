package ru.gravit.launchserver.auth.provider;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.serialize.config.ConfigObject;
import ru.gravit.launcher.serialize.config.TextConfigReader;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.ConfigEntry;
import ru.gravit.launcher.serialize.config.entry.ConfigEntry.Type;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;

public final class FileAuthProvider extends DigestAuthProvider {
    private static final class Entry extends ConfigObject {
        private final String username;
        private final String password;
        private final String ip;

        private Entry(BlockConfigEntry block) {
            super(block);
            username = VerifyHelper.verifyUsername(block.getEntryValue("username", StringConfigEntry.class));
            password = VerifyHelper.verify(block.getEntryValue("password", StringConfigEntry.class),
                    VerifyHelper.NOT_EMPTY, String.format("Password can't be empty: '%s'", username));
            ip = block.hasEntry("ip") ? VerifyHelper.verify(block.getEntryValue("ip", StringConfigEntry.class),
                    VerifyHelper.NOT_EMPTY, String.format("IP can't be empty: '%s'", username)) : null;
        }
    }

    private final Path file;
    // Cache
    private final Map<String, Entry> entries = new HashMap<>(256);
    private final Object cacheLock = new Object();

    private FileTime cacheLastModified;

    public FileAuthProvider(BlockConfigEntry block) {
        super(block);
        file = IOHelper.toPath(block.getEntryValue("file", StringConfigEntry.class));

        // Try to update cache
        try {
            updateCache();
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws IOException {
        Entry entry;
        synchronized (cacheLock) {
            updateCache();
            entry = entries.get(CommonHelper.low(login));
        }

        // Verify digest and return true username
        verifyDigest(entry == null ? null : entry.password, password);
        if (entry == null || entry.ip != null && !entry.ip.equals(ip))
            authError("Authentication from this IP is not allowed");

        // We're done
        return new AuthProviderResult(entry.username, SecurityHelper.randomStringToken());
    }

    @Override
    public void close() {
        // Do nothing
    }

    private void updateCache() throws IOException {
        FileTime lastModified = IOHelper.readAttributes(file).lastModifiedTime();
        if (lastModified.equals(cacheLastModified))
            return; // Not modified, so cache is up-to-date

        // Read file
        LogHelper.info("Recaching auth provider file: '%s'", file);
        BlockConfigEntry authFile;
        try (BufferedReader reader = IOHelper.newReader(file)) {
            authFile = TextConfigReader.read(reader, false);
        }

        // Read entries from config block
        entries.clear();
        Set<Map.Entry<String, ConfigEntry<?>>> entrySet = authFile.getValue().entrySet();
        for (Map.Entry<String, ConfigEntry<?>> entry : entrySet) {
            String login = entry.getKey();
            ConfigEntry<?> value = VerifyHelper.verify(entry.getValue(), v -> v.getType() == Type.BLOCK,
                    String.format("Illegal config entry type: '%s'", login));

            // Add auth entry
            Entry auth = new Entry((BlockConfigEntry) value);
            VerifyHelper.putIfAbsent(entries, CommonHelper.low(login), auth,
                    String.format("Duplicate login: '%s'", login));
        }

        // Update last modified time
        cacheLastModified = lastModified;
    }
}
