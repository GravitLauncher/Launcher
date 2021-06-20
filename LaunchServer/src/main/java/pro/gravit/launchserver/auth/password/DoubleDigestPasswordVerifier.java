package pro.gravit.launchserver.auth.password;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class DoubleDigestPasswordVerifier extends PasswordVerifier {
    private transient final Logger logger = LogManager.getLogger();
    public String algo;
    public boolean toHexMode;

    private byte[] digest(String text) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algo);
        byte[] firstDigest = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        return toHexMode ? digest.digest(SecurityHelper.toHex(firstDigest).getBytes(StandardCharsets.UTF_8)) : digest.digest(firstDigest);
    }

    @Override
    public boolean check(String encryptedPassword, String password) {
        try {
            byte[] bytes = SecurityHelper.fromHex(encryptedPassword);
            return Arrays.equals(bytes, digest(password));
        } catch (NoSuchAlgorithmException e) {
            logger.error("Digest algorithm {} not supported", algo);
            return false;
        }
    }

    @Override
    public String encrypt(String password) {
        try {
            return SecurityHelper.toHex(digest(password));
        } catch (NoSuchAlgorithmException e) {
            logger.error("Digest algorithm {} not supported", algo);
            return null;
        }
    }
}
