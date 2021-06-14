package pro.gravit.launchserver.auth.password;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.utils.helper.SecurityHelper;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class DigestPasswordVerifier extends PasswordVerifier {
    private transient final Logger logger = LogManager.getLogger();
    public String algo;

    @Override
    public boolean check(String encryptedPassword, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algo);
            digest.update(password.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = digest.digest();
            String myHash = DatatypeConverter
                    .printHexBinary(bytes);
            return myHash.equalsIgnoreCase(encryptedPassword);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Digest algorithm {} not supported", algo);
            return false;
        }
    }
}
