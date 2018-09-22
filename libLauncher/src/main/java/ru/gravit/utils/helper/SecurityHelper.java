package ru.gravit.utils.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import ru.gravit.launcher.LauncherAPI;

public final class SecurityHelper {
    @LauncherAPI
    public enum DigestAlgorithm {
        PLAIN("plain", -1), MD5("MD5", 128), SHA1("SHA-1", 160), SHA224("SHA-224", 224), SHA256("SHA-256", 256), SHA512("SHA-512", 512);
        private static final Map<String, DigestAlgorithm> ALGORITHMS;

        static {
            DigestAlgorithm[] algorithmsValues = values();
            ALGORITHMS = new HashMap<>(algorithmsValues.length);
            for (DigestAlgorithm algorithm : algorithmsValues)
                ALGORITHMS.put(algorithm.name, algorithm);
        }

        public static DigestAlgorithm byName(String name) {
            return VerifyHelper.getMapValue(ALGORITHMS, name, String.format("Unknown digest algorithm: '%s'", name));
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

    // Algorithm constants
    @LauncherAPI
    public static final String RSA_ALGO = "RSA";
    @LauncherAPI
    public static final String RSA_SIGN_ALGO = "SHA256withRSA";

    @LauncherAPI
    public static final String RSA_CIPHER_ALGO = "RSA/ECB/PKCS1Padding";
    // Algorithm size constants
    @LauncherAPI
    public static final int TOKEN_LENGTH = 16;
    @LauncherAPI
    public static final int TOKEN_STRING_LENGTH = TOKEN_LENGTH << 1;
    @LauncherAPI
    public static final int RSA_KEY_LENGTH_BITS = 2048;
    @LauncherAPI
    public static final int RSA_KEY_LENGTH = RSA_KEY_LENGTH_BITS / Byte.SIZE;

    @LauncherAPI
    public static final int CRYPTO_MAX_LENGTH = 2048;
    // Certificate constants
    @LauncherAPI
    public static final String HEX = "0123456789abcdef";

    @LauncherAPI
    public static final SecureRandom secureRandom = new SecureRandom();
    // Random generator constants
    private static final char[] VOWELS = {'e', 'u', 'i', 'o', 'a'};

    private static final char[] CONS = {'r', 't', 'p', 's', 'd', 'f', 'g', 'h', 'k', 'l', 'c', 'v', 'b', 'n', 'm'};

    @LauncherAPI
    public static byte[] digest(DigestAlgorithm algo, byte[] bytes) {
        return newDigest(algo).digest(bytes);
    }

    @LauncherAPI
    public static byte[] digest(DigestAlgorithm algo, InputStream input) throws IOException {
        byte[] buffer = IOHelper.newBuffer();
        MessageDigest digest = newDigest(algo);
        for (int length = input.read(buffer); length != -1; length = input.read(buffer))
            digest.update(buffer, 0, length);
        return digest.digest();
    }

    @LauncherAPI
    public static byte[] digest(DigestAlgorithm algo, Path file) throws IOException {
        try (InputStream input = IOHelper.newInput(file)) {
            return digest(algo, input);
        }
    }

    @LauncherAPI
    public static byte[] digest(DigestAlgorithm algo, String s) {
        return digest(algo, IOHelper.encode(s));
    }

    @LauncherAPI
    public static byte[] digest(DigestAlgorithm algo, URL url) throws IOException {
        try (InputStream input = IOHelper.newInput(url)) {
            return digest(algo, input);
        }
    }

    @LauncherAPI
    public static KeyPair genRSAKeyPair() {
        return genRSAKeyPair(newRandom());
    }

    @LauncherAPI
    public static KeyPair genRSAKeyPair(SecureRandom random) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA_ALGO);
            generator.initialize(RSA_KEY_LENGTH_BITS, random);
            return generator.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError(e);
        }
    }

    @LauncherAPI
    public static boolean isValidSign(byte[] bytes, byte[] sign, RSAPublicKey publicKey) throws SignatureException {
        Signature signature = newRSAVerifySignature(publicKey);
        try {
            signature.update(bytes);
        } catch (SignatureException e) {
            throw new InternalError(e);
        }
        return signature.verify(sign);
    }

    @LauncherAPI
    public static boolean isValidSign(InputStream input, byte[] sign, RSAPublicKey publicKey) throws IOException, SignatureException {
        Signature signature = newRSAVerifySignature(publicKey);
        updateSignature(input, signature);
        return signature.verify(sign);
    }

    @LauncherAPI
    public static boolean isValidSign(Path path, byte[] sign, RSAPublicKey publicKey) throws IOException, SignatureException {
        try (InputStream input = IOHelper.newInput(path)) {
            return isValidSign(input, sign, publicKey);
        }
    }

    @LauncherAPI
    public static boolean isValidSign(URL url, byte[] sign, RSAPublicKey publicKey) throws IOException, SignatureException {
        try (InputStream input = IOHelper.newInput(url)) {
            return isValidSign(input, sign, publicKey);
        }
    }

    @LauncherAPI
    public static boolean isValidToken(CharSequence token) {
        return token.length() == TOKEN_STRING_LENGTH && token.chars().allMatch(ch -> HEX.indexOf(ch) >= 0);
    }

    private static Cipher newCipher(String algo) {
        // IDK Why, but collapsing catch blocks makes ProGuard generate invalid stackmap
        try {
            return Cipher.getInstance(algo);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new InternalError(e);
        }
    }

    @LauncherAPI
    public static MessageDigest newDigest(DigestAlgorithm algo) {
        VerifyHelper.verify(algo, a -> a != DigestAlgorithm.PLAIN, "PLAIN digest");
        try {
            return MessageDigest.getInstance(algo.name);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError(e);
        }
    }

    @LauncherAPI
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

    @LauncherAPI
    public static Cipher newRSADecryptCipher(RSAPrivateKey key) {
        return newRSACipher(Cipher.DECRYPT_MODE, key);
    }

    @LauncherAPI
    public static Cipher newRSAEncryptCipher(RSAPublicKey key) {
        return newRSACipher(Cipher.ENCRYPT_MODE, key);
    }

    private static KeyFactory newRSAKeyFactory() {
        try {
            return KeyFactory.getInstance(RSA_ALGO);
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

    @LauncherAPI
    public static Signature newRSASignSignature(RSAPrivateKey key) {
        Signature signature = newRSASignature();
        try {
            signature.initSign(key);
        } catch (InvalidKeyException e) {
            throw new InternalError(e);
        }
        return signature;
    }

    @LauncherAPI
    public static Signature newRSAVerifySignature(RSAPublicKey key) {
        Signature signature = newRSASignature();
        try {
            signature.initVerify(key);
        } catch (InvalidKeyException e) {
            throw new InternalError(e);
        }
        return signature;
    }

    @LauncherAPI
    public static byte[] randomBytes(int length) {
        return randomBytes(newRandom(), length);
    }

    @LauncherAPI
    public static byte[] randomBytes(Random random, int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    @LauncherAPI
    public static String randomStringToken() {
        return randomStringToken(newRandom());
    }

    @LauncherAPI
    public static String randomStringToken(Random random) {
        return toHex(randomToken(random));
    }

    @LauncherAPI
    public static byte[] randomToken() {
        return randomToken(newRandom());
    }

    @LauncherAPI
    public static byte[] randomToken(Random random) {
        return randomBytes(random, TOKEN_LENGTH);
    }

    @LauncherAPI
    public static String randomUsername() {
        return randomUsername(newRandom());
    }

    @LauncherAPI
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

    @LauncherAPI
    public static byte[] sign(byte[] bytes, RSAPrivateKey privateKey) {
        Signature signature = newRSASignSignature(privateKey);
        try {
            signature.update(bytes);
            return signature.sign();
        } catch (SignatureException e) {
            throw new InternalError(e);
        }
    }

    @LauncherAPI
    public static byte[] sign(InputStream input, RSAPrivateKey privateKey) throws IOException {
        Signature signature = newRSASignSignature(privateKey);
        updateSignature(input, signature);
        try {
            return signature.sign();
        } catch (SignatureException e) {
            throw new InternalError(e);
        }
    }

    @LauncherAPI
    public static byte[] sign(Path path, RSAPrivateKey privateKey) throws IOException {
        try (InputStream input = IOHelper.newInput(path)) {
            return sign(input, privateKey);
        }
    }

    @LauncherAPI
    public static String toHex(byte[] bytes) {
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

    @LauncherAPI
    public static RSAPrivateKey toPrivateRSAKey(byte[] bytes) throws InvalidKeySpecException {
        return (RSAPrivateKey) newRSAKeyFactory().generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    @LauncherAPI
    public static RSAPublicKey toPublicRSAKey(byte[] bytes) throws InvalidKeySpecException {
        return (RSAPublicKey) newRSAKeyFactory().generatePublic(new X509EncodedKeySpec(bytes));
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

    @LauncherAPI
    public static void verifySign(byte[] bytes, byte[] sign, RSAPublicKey publicKey) throws SignatureException {
        if (!isValidSign(bytes, sign, publicKey))
            throw new SignatureException("Invalid sign");
    }

    @LauncherAPI
    public static void verifySign(InputStream input, byte[] sign, RSAPublicKey publicKey) throws SignatureException, IOException {
        if (!isValidSign(input, sign, publicKey))
            throw new SignatureException("Invalid stream sign");
    }

    @LauncherAPI
    public static void verifySign(Path path, byte[] sign, RSAPublicKey publicKey) throws SignatureException, IOException {
        if (!isValidSign(path, sign, publicKey))
            throw new SignatureException(String.format("Invalid file sign: '%s'", path));
    }

    @LauncherAPI
    public static void verifySign(URL url, byte[] sign, RSAPublicKey publicKey) throws SignatureException, IOException {
        if (!isValidSign(url, sign, publicKey))
            throw new SignatureException(String.format("Invalid URL sign: '%s'", url));
    }

    @LauncherAPI
    public static String verifyToken(String token) {
        return VerifyHelper.verify(token, SecurityHelper::isValidToken, String.format("Invalid token: '%s'", token));
    }

    private SecurityHelper() {
    }
}
