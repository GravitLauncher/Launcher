package ru.gravit.launcher.serialize.signed;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.stream.StreamObject;
import ru.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class SignedBytesHolder extends StreamObject {
    protected final byte[] bytes;
    private final byte[] sign;

    @LauncherAPI
    public SignedBytesHolder(byte[] bytes, byte[] sign, RSAPublicKey publicKey) throws SignatureException {
        SecurityHelper.verifySign(bytes, sign, publicKey);
        this.bytes = bytes.clone();
        this.sign = sign.clone();
    }

    @LauncherAPI
    public SignedBytesHolder(byte[] bytes, RSAPrivateKey privateKey) {
        this.bytes = bytes.clone();
        sign = SecurityHelper.sign(bytes, privateKey);
    }

    @LauncherAPI
    public SignedBytesHolder(HInput input, RSAPublicKey publicKey) throws IOException, SignatureException {
        this(input.readByteArray(0), input.readByteArray(-SecurityHelper.RSA_KEY_LENGTH), publicKey);
    }

    @LauncherAPI
    public final byte[] getBytes() {
        return bytes.clone();
    }

    @LauncherAPI
    public final byte[] getSign() {
        return sign.clone();
    }

    @Override
    public final void write(HOutput output) throws IOException {
        output.writeByteArray(bytes, 0);
        output.writeByteArray(sign, -SecurityHelper.RSA_KEY_LENGTH);
    }
}
