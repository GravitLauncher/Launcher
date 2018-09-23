package ru.gravit.launchserver.auth.provider;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.SecurityHelper.DigestAlgorithm;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;
import ru.gravit.launchserver.auth.AuthException;

public abstract class DigestAuthProvider extends AuthProvider {
    private final DigestAlgorithm digest;

    @LauncherAPI
    protected DigestAuthProvider(BlockConfigEntry block, LaunchServer server) {
        super(block,server);
        digest = DigestAlgorithm.byName(block.getEntryValue("digest", StringConfigEntry.class));
    }

    @LauncherAPI
    protected final void verifyDigest(String validDigest, String password) throws AuthException {
        boolean valid;
        if (digest == DigestAlgorithm.PLAIN)
            valid = password.equals(validDigest);
        else if (validDigest == null)
            valid = false;
        else {
            byte[] actualDigest = SecurityHelper.digest(digest, password);
            valid = SecurityHelper.toHex(actualDigest).equals(validDigest);
        }

        // Verify is valid
        if (!valid)
            authError("Incorrect username or password");
    }
}
