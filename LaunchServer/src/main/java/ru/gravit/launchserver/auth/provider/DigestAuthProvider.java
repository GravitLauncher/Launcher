package ru.gravit.launchserver.auth.provider;

import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.SecurityHelper.DigestAlgorithm;

public abstract class DigestAuthProvider extends AuthProvider {
    private DigestAlgorithm digest;


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
