package pro.gravit.utils.helper;

import java.util.Objects;
import java.util.function.LongSupplier;

public final class CryptoHelper {
    private CryptoHelper() {
    }

    public static byte[] encode(byte[] txt, String pKey) {
        Objects.requireNonNull(txt);
        Objects.requireNonNull(pKey);
        byte[] key = SecurityHelper.fromHex(pKey);
        byte[] res = new byte[txt.length];
        for (int i = 0; i < txt.length; i++)
            res[i] = (byte) (txt[i] ^ key[i % key.length]);
        return res;
    }

    public static byte[] decode(byte[] pText, String pKey) {
        Objects.requireNonNull(pText);
        Objects.requireNonNull(pKey);
        byte[] res = new byte[pText.length];
        byte[] key = SecurityHelper.fromHex(pKey);
        for (int i = 0; i < pText.length; i++)
            res[i] = (byte) (pText[i] ^ key[i % key.length]);
        return res;
    }

    public static void encodeOrig(byte[] txt, String pKey) {
        Objects.requireNonNull(txt);
        Objects.requireNonNull(pKey);
        byte[] key = SecurityHelper.fromHex(pKey);
        for (int i = 0; i < txt.length; i++)
            txt[i] = (byte) (txt[i] ^ key[i % key.length]);
    }

    public static void decodeOrig(byte[] pText, String pKey) {
        Objects.requireNonNull(pText);
        Objects.requireNonNull(pKey);
        byte[] key = SecurityHelper.fromHex(pKey);
        for (int i = 0; i < pText.length; i++)
            pText[i] = (byte) (pText[i] ^ key[i % key.length]);
    }

    public static String randomToken(int depth) {
        VerifyHelper.verifyInt(depth, VerifyHelper.POSITIVE, "Depth must be positive");
        return SecurityHelper.toHex(SecurityHelper.randomBytes(SecurityHelper.TOKEN_LENGTH * depth));
    }

    public static class StaticRandom implements LongSupplier {
        private volatile long rnd;

        public StaticRandom(long rnd) {
            this.rnd = rnd;
        }

        @Override
        public long getAsLong() {
            this.rnd ^= (this.rnd << 21);
            this.rnd ^= (this.rnd >>> 35);
            this.rnd ^= (this.rnd << 4);
            return this.rnd;
        }
    }
}
