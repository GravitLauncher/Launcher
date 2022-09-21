package pro.gravit.launchserver.auth.password;

import pro.gravit.utils.ProviderMap;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public abstract class PasswordVerifier {
    public static final ProviderMap<PasswordVerifier> providers = new ProviderMap<>("PasswordVerifier");
    private static boolean registeredProviders = false;

    public static void registerProviders() {
        if (!registeredProviders) {
            providers.register("plain", PlainPasswordVerifier.class);
            providers.register("digest", DigestPasswordVerifier.class);
            providers.register("doubleDigest", DoubleDigestPasswordVerifier.class);
            providers.register("json", JsonPasswordVerifier.class);
            providers.register("accept", AcceptPasswordVerifier.class);
            providers.register("reject", RejectPasswordVerifier.class);
            providers.register("django", DjangoPasswordVerifier.class);
            registeredProviders = true;
        }
    }

    public abstract boolean check(String encryptedPassword, String password) throws NoSuchAlgorithmException, InvalidKeySpecException;

    public String encrypt(String password) {
        throw new UnsupportedOperationException();
    }
}
