package pro.gravit.utils.helper;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class SecurityHelper {

    public static final String EC_ALGO = "EC";

    // EC Algorithm constants
    public static final String EC_CURVE = "secp256r1";
    public static final String EC_SIGN_ALGO = "SHA256withECDSA";
    public static final int TOKEN_LENGTH = 16;

    // RSA Algorithm constants

    public static final String RSA_ALGO = "RSA";
    public static final String RSA_SIGN_ALGO = "SHA256withRSA";
    public static final String RSA_CIPHER_ALGO = "RSA/ECB/PKCS1Padding";

    // Algorithm size constants
    public static final int AES_KEY_LENGTH = 8;
    public static final int TOKEN_STRING_LENGTH = TOKEN_LENGTH << 1;
    public static final int RSA_KEY_LENGTH_BITS = 2048;
    public static final int RSA_KEY_LENGTH = RSA_KEY_LENGTH_BITS / Byte.SIZE;
    public static final int CRYPTO_MAX_LENGTH = 2048;
    public static final String HEX = "0123456789abcdef";
    // Certificate constants
    public static final byte[] NUMBERS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    public static final SecureRandom secureRandom = new SecureRandom();
    // Random generator constants
    private static final char[] VOWELS = {'e', 'u', 'i', 'o', 'a'};
    private static final char[] CONS = {'r', 't', 'p', 's', 'd', 'f', 'g', 'h', 'k', 'l', 'c', 'v', 'b', 'n', 'm'};

    private SecurityHelper() {
    }

    public static byte[] digest(DigestAlgorithm algo, byte[] bytes) {
        return newDigest(algo).digest(bytes);
    }


    public static byte[] digest(DigestAlgorithm algo, InputStream input) throws IOException {
        byte[] buffer = IOHelper.newBuffer();
        MessageDigest digest = newDigest(algo);
        for (int length = input.read(buffer); length != -1; length = input.read(buffer))
            digest.update(buffer, 0, length);
        return digest.digest();
    }


    public static byte[] digest(DigestAlgorithm algo, Path file) throws IOException {
        try (InputStream input = IOHelper.newInput(file)) {
            return digest(algo, input);
        }
    }


    public static byte[] digest(DigestAlgorithm algo, String s) {
        return digest(algo, IOHelper.encode(s));
    }


    public static byte[] digest(DigestAlgorithm algo, URL url) throws IOException {
        try (InputStream input = IOHelper.newInput(url)) {
            return digest(algo, input);
        }
    }

    public static KeyPair genECDSAKeyPair(SecureRandom random) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(EC_ALGO);
            generator.initialize(new ECGenParameterSpec(EC_CURVE), random);
            return generator.genKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new InternalError(e);
        }
    }

    public static KeyPair genRSAKeyPair(SecureRandom random) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA_ALGO);
            generator.initialize(RSA_KEY_LENGTH_BITS, random);
            return generator.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError(e);
        }
    }


    public static boolean isValidSign(byte[] bytes, byte[] sign, ECPublicKey publicKey) throws SignatureException {
        Signature signature = newECVerifySignature(publicKey);
        try {
            signature.update(bytes);
        } catch (SignatureException e) {
            throw new InternalError(e);
        }
        return signature.verify(sign);
    }


    public static boolean isValidSign(InputStream input, byte[] sign, ECPublicKey publicKey) throws IOException, SignatureException {
        Signature signature = newECVerifySignature(publicKey);
        updateSignature(input, signature);
        return signature.verify(sign);
    }

    public static boolean isValidSign(byte[] bytes, byte[] sign, RSAPublicKey publicKey) throws SignatureException {
        Signature signature = newRSAVerifySignature(publicKey);
        try {
            signature.update(bytes);
        } catch (SignatureException e) {
            throw new InternalError(e);
        }
        return signature.verify(sign);
    }


    public static boolean isValidSign(InputStream input, byte[] sign, RSAPublicKey publicKey) throws IOException, SignatureException {
        Signature signature = newRSAVerifySignature(publicKey);
        updateSignature(input, signature);
        return signature.verify(sign);
    }


    public static boolean isValidToken(CharSequence token) {
        return token.length() == TOKEN_STRING_LENGTH && token.chars().allMatch(ch -> HEX.indexOf(ch) >= 0);
    }

    public static Cipher newCipher(String algo) {
        try {
            return Cipher.getInstance(algo);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new InternalError(e);
        }
    }

    /**
     * @param algo Cipher algo
     * @return Cipher instance
     * @throws SecurityException: JCE cannot authenticate the provider BC if BouncyCastle is in unsigned jar
     */
    private static Cipher newBCCipher(String algo) {
        try {
            return Cipher.getInstance(algo, "BC");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | NoSuchProviderException e) {
            throw new InternalError(e);
        }
    }


    public static MessageDigest newDigest(DigestAlgorithm algo) {
        VerifyHelper.verify(algo, a -> a != DigestAlgorithm.PLAIN, "PLAIN digest");
        try {
            return MessageDigest.getInstance(algo.name);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError(e);
        }
    }


    public static SecureRandom newRandom() {
        return new SecureRandom();
    }

    private static Cipher newRSACipher(int mode, RSAKey key) {
        Cipher cipher = newCipher(RSA_CIPHER_ALGO);
        try {
            cipher.init(mode, (Key) key);
        } catch (InvalidKeyException e) {
            throw new InternalError(e);
        }
        return cipher;
    }

    private static KeyFactory newECDSAKeyFactory() {
        try {
            return KeyFactory.getInstance(EC_ALGO);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError(e);
        }
    }

    private static KeyFactory newRSAKeyFactory() {
        try {
            return KeyFactory.getInstance(RSA_ALGO);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError(e);
        }
    }

    private static Signature newECSignature() {
        try {
            return Signature.getInstance(EC_SIGN_ALGO);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError(e);
        }
    }

    private static Signature newRSASignature() {
        try {
            return Signature.getInstance(RSA_SIGN_ALGO);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError(e);
        }
    }

    public static Signature newECSignSignature(ECPrivateKey key) {
        Signature signature = newECSignature();
        try {
            signature.initSign(key);
        } catch (InvalidKeyException e) {
            throw new InternalError(e);
        }
        return signature;
    }

    public static Signature newRSASignSignature(RSAPrivateKey key) {
        Signature signature = newRSASignature();
        try {
            signature.initSign(key);
        } catch (InvalidKeyException e) {
            throw new InternalError(e);
        }
        return signature;
    }


    public static Signature newECVerifySignature(ECPublicKey key) {
        Signature signature = newECSignature();
        try {
            signature.initVerify(key);
        } catch (InvalidKeyException e) {
            throw new InternalError(e);
        }
        return signature;
    }

    public static Signature newRSAVerifySignature(RSAPublicKey key) {
        Signature signature = newRSASignature();
        try {
            signature.initVerify(key);
        } catch (InvalidKeyException e) {
            throw new InternalError(e);
        }
        return signature;
    }


    public static byte[] randomBytes(int length) {
        return randomBytes(newRandom(), length);
    }


    public static byte[] randomBytes(Random random, int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }


    public static String randomStringToken() {
        return randomStringToken(newRandom());
    }


    public static String randomStringToken(Random random) {
        return toHex(randomToken(random));
    }


    public static byte[] randomToken() {
        return randomToken(newRandom());
    }


    public static byte[] randomToken(Random random) {
        return randomBytes(random, TOKEN_LENGTH);
    }


    public static String randomStringAESKey() {
        return toHex(randomAESKey(newRandom()));
    }

    public static String randomStringAESKey(Random random) {
        return toHex(randomAESKey(random));
    }


    public static byte[] randomAESKey() {
        return randomAESKey(newRandom());
    }


    public static byte[] randomAESKey(Random random) {
        return randomBytes(random, AES_KEY_LENGTH);
    }


    public static String randomUsername() {
        return randomUsername(newRandom());
    }


    public static String randomUsername(Random random) {
        int usernameLength = 3 + random.nextInt(7); // 3-9

        // Choose prefix
        String prefix;
        int prefixType = random.nextInt(7);
        if (usernameLength >= 5 && prefixType == 6) { // (6) 2-char
            prefix = random.nextBoolean() ? "Mr" : "Dr";
            usernameLength -= 2;
        } else if (usernameLength >= 6 && prefixType == 5) { // (5) 3-char
            prefix = "Mrs";
            usernameLength -= 3;
        } else
            prefix = "";

        // Choose suffix
        String suffix;
        int suffixType = random.nextInt(7); // 0-6, 7 values
        if (usernameLength >= 5 && suffixType == 6) { // (6) 10-99
            suffix = String.valueOf(10 + random.nextInt(90));
            usernameLength -= 2;
        } else if (usernameLength >= 7 && suffixType == 5) { // (5) 1990-2015
            suffix = String.valueOf(1990 + random.nextInt(26));
            usernameLength -= 4;
        } else
            suffix = "";

        // Choose name
        int consRepeat = 0;
        boolean consPrev = random.nextBoolean();
        char[] chars = new char[usernameLength];
        for (int i = 0; i < chars.length; i++) {
            if (i > 1 && consPrev && random.nextInt(10) == 0) { // Doubled
                chars[i] = chars[i - 1];
                continue;
            }

            // Choose next char
            if (consRepeat < 1 && random.nextInt() == 5)
                consRepeat++;
            else {
                consRepeat = 0;
                consPrev ^= true;
            }

            // Choose char
            char[] alphabet = consPrev ? CONS : VOWELS;
            chars[i] = alphabet[random.nextInt(alphabet.length)];
        }

        // Make first letter uppercase
        if (!prefix.isEmpty() || random.nextBoolean())
            chars[0] = Character.toUpperCase(chars[0]);

        // Return chosen name (and verify for sure)
        return VerifyHelper.verifyUsername(prefix + new String(chars) + suffix);
    }

    public static byte[] sign(byte[] bytes, ECPrivateKey privateKey) {
        Signature signature = newECSignSignature(privateKey);
        try {
            signature.update(bytes);
            return signature.sign();
        } catch (SignatureException e) {
            throw new InternalError(e);
        }
    }


    public static String toHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        int offset = 0;
        char[] hex = new char[bytes.length << 1];
        for (byte currentByte : bytes) {
            int ub = Byte.toUnsignedInt(currentByte);
            hex[offset] = HEX.charAt(ub >>> 4);
            offset++;
            hex[offset] = HEX.charAt(ub & 0x0F);
            offset++;
        }
        return new String(hex);
    }

    public static ECPublicKey toPublicECDSAKey(byte[] bytes) throws InvalidKeySpecException {
        return (ECPublicKey) newECDSAKeyFactory().generatePublic(new X509EncodedKeySpec(bytes));
    }

    public static ECPrivateKey toPrivateECDSAKey(byte[] bytes) throws InvalidKeySpecException {
        return (ECPrivateKey) newECDSAKeyFactory().generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    public static RSAPublicKey toPublicRSAKey(byte[] bytes) throws InvalidKeySpecException {
        return (RSAPublicKey) newRSAKeyFactory().generatePublic(new X509EncodedKeySpec(bytes));
    }

    public static RSAPrivateKey toPrivateRSAKey(byte[] bytes) throws InvalidKeySpecException {
        return (RSAPrivateKey) newRSAKeyFactory().generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    private static void updateSignature(InputStream input, Signature signature) throws IOException {
        byte[] buffer = IOHelper.newBuffer();
        for (int length = input.read(buffer); length >= 0; length = input.read(buffer))
            try {
                signature.update(buffer, 0, length);
            } catch (SignatureException e) {
                throw new InternalError(e);
            }
    }


    public static void verifySign(byte[] bytes, byte[] sign, ECPublicKey publicKey) throws SignatureException {
        if (!isValidSign(bytes, sign, publicKey))
            throw new SignatureException("Invalid sign");
    }


    public static void verifySign(InputStream input, byte[] sign, ECPublicKey publicKey) throws SignatureException, IOException {
        if (!isValidSign(input, sign, publicKey))
            throw new SignatureException("Invalid stream sign");
    }

    public static void verifySign(byte[] bytes, byte[] sign, RSAPublicKey publicKey) throws SignatureException {
        if (!isValidSign(bytes, sign, publicKey))
            throw new SignatureException("Invalid sign");
    }


    public static void verifySign(InputStream input, byte[] sign, RSAPublicKey publicKey) throws SignatureException, IOException {
        if (!isValidSign(input, sign, publicKey))
            throw new SignatureException("Invalid stream sign");
    }


    public static String verifyToken(String token) {
        return VerifyHelper.verify(token, SecurityHelper::isValidToken, String.format("Invalid token: '%s'", token));
    }

    public static Cipher newRSADecryptCipher(RSAPrivateKey privateKey) {
        try {
            return newRSACipher(Cipher.DECRYPT_MODE, privateKey);
        } catch (SecurityException e) {
            throw new InternalError(e);
        }
    }

    public static Cipher newRSAEncryptCipher(RSAPublicKey publicKey) {
        try {
            return newRSACipher(Cipher.ENCRYPT_MODE, publicKey);
        } catch (SecurityException e) {
            throw new InternalError(e);
        }
    }

    //AES
    public static byte[] encrypt(String seed, byte[] cleartext) throws Exception {
        byte[] rawKey = getAESKey(IOHelper.encode(seed));
        return encrypt(rawKey, cleartext);
    }

    public static byte[] encrypt(String seed, String cleartext) throws Exception {
        return encrypt(seed, IOHelper.encode(cleartext));
    }

    public static byte[] getAESKey(byte[] seed) throws Exception {
        KeyGenerator kGen = KeyGenerator.getInstance("AES");
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(seed);
        kGen.init(128, sr); // 192 and 256 bits may not be available
        SecretKey sKey = kGen.generateKey();
        return sKey.getEncoded();
    }

    public static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
        SecretKeySpec sKeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, sKeySpec);
        return cipher.doFinal(clear);
    }

    public static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
        SecretKeySpec sKeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, sKeySpec);
        return cipher.doFinal(encrypted);
    }

    public static byte[] decrypt(String seed, byte[] encrypted) throws Exception {
        return decrypt(getAESKey(IOHelper.encode(seed)), encrypted);
    }

    public static byte[] fromHex(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).byteValue();
        }
        return result;
    }

    public enum DigestAlgorithm {
        PLAIN("plain", -1), MD5("MD5", 128), SHA1("SHA-1", 160), SHA224("SHA-224", 224), SHA256("SHA-256", 256), SHA512("SHA-512", 512);
        private static final Map<String, DigestAlgorithm> ALGORITHMS;

        static {
            DigestAlgorithm[] algorithmsValues = values();
            ALGORITHMS = new HashMap<>(algorithmsValues.length);
            for (DigestAlgorithm algorithm : algorithmsValues)
                ALGORITHMS.put(algorithm.name, algorithm);
        }

        // Instance
        public final String name;
        public final int bits;
        public final int bytes;

        DigestAlgorithm(String name, int bits) {
            this.name = name;
            this.bits = bits;

            // Convert to bytes
            bytes = bits / Byte.SIZE;
            assert bits % Byte.SIZE == 0;
        }

        public static DigestAlgorithm byName(String name) {
            return VerifyHelper.getMapValue(ALGORITHMS, name, String.format("Unknown digest algorithm: '%s'", name));
        }

        @Override
        public String toString() {
            return name;
        }

        public byte[] verify(byte[] digest) {
            if (digest.length != bytes)
                throw new IllegalArgumentException("Invalid digest length: " + digest.length);
            return digest;
        }
    }
}
