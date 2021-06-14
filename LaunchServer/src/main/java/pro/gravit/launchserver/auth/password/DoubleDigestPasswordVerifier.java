package pro.gravit.launchserver.auth.password;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.utils.helper.SecurityHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class DoubleDigestPasswordVerifier extends PasswordVerifier {
    private transient final Logger logger = LogManager.getLogger();
    public String algo;
    public boolean toHexMode;

    @Override
    public boolean check(String encryptedPassword, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algo);
            byte[] bytes = SecurityHelper.fromHex(password);
            byte[] firstDigest = digest.digest(bytes);
            return Arrays.equals(encryptedPassword.getBytes(StandardCharsets.UTF_8), toHexMode ? digest.digest(SecurityHelper.toHex(firstDigest).getBytes(StandardCharsets.UTF_8)) : digest.digest(firstDigest));
        } catch (NoSuchAlgorithmException e) {
            logger.error("Digest algorithm {} not supported", algo);
            return false;
        }
    }
}
