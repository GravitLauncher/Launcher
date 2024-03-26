package pro.gravit.launchserver.auth.password;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class DjangoPasswordVerifier extends PasswordVerifier {
    public final Integer DEFAULT_ITERATIONS = 10000;
    private static final Logger logger = LogManager.getLogger();
    private static final String algorithm = "pbkdf2_sha256";

    public String getEncodedHash(String password, String salt, int iterations) {
        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
        generator.init(password.getBytes(StandardCharsets.UTF_8), salt.getBytes(), iterations);
        byte[] dk = ((KeyParameter) generator.generateDerivedParameters(256)).getKey();
        byte[] hashBase64 = Base64.getEncoder().encode(dk);
        return new String(hashBase64);
    }

    public String encode(String password, String salt, int iterations) {
        String hash = getEncodedHash(password, salt, iterations);
        return String.format("%s$%d$%s$%s", algorithm, iterations, salt, hash);
    }

    @Override
    public boolean check(String encryptedPassword, String password) {
        String[] params = encryptedPassword.split("\\$");
        if (params.length != 4) {
            logger.warn(" end 1 " + params.length);
            return false;
        }
        int iterations = Integer.parseInt(params[1]);
        String salt = params[2];
        String hash = encode(password, salt, iterations);
        return hash.equals(encryptedPassword);
    }
}
