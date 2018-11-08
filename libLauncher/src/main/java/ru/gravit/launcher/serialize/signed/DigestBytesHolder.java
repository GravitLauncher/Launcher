package ru.gravit.launcher.serialize.signed;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.stream.StreamObject;
import ru.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.security.SignatureException;
import java.util.Arrays;

public class DigestBytesHolder extends StreamObject {
    protected final byte[] bytes;
    private final byte[] digest;

    @LauncherAPI
    public DigestBytesHolder(byte[] bytes, byte[] digest, SecurityHelper.DigestAlgorithm algorithm) throws SignatureException {
        if (Arrays.equals(SecurityHelper.digest(algorithm, bytes), digest))
            throw new SignatureException("Invalid digest");
        this.bytes = bytes.clone();
        this.digest = digest.clone();
    }

    @LauncherAPI
    public DigestBytesHolder(byte[] bytes, SecurityHelper.DigestAlgorithm algorithm) {
        this.bytes = bytes.clone();
        this.digest = SecurityHelper.digest(algorithm, bytes);
    }

    @LauncherAPI
    public DigestBytesHolder(HInput input, SecurityHelper.DigestAlgorithm algorithm) throws IOException, SignatureException {
        this(input.readByteArray(0), input.readByteArray(-SecurityHelper.RSA_KEY_LENGTH), algorithm);
    }

    @LauncherAPI
    public final byte[] getBytes() {
        return bytes.clone();
    }

    @LauncherAPI
    public final byte[] getDigest() {
        return digest.clone();
    }

    @Override
    public final void write(HOutput output) throws IOException {
        output.writeByteArray(bytes, 0);
        output.writeByteArray(digest, -SecurityHelper.RSA_KEY_LENGTH);
    }
}
