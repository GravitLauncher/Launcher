package pro.gravit.launcher.hasher;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.utils.helper.SecurityHelper.DigestAlgorithm;
import pro.gravit.utils.helper.VerifyHelper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public final class HashedFile extends HashedEntry {
    public static final DigestAlgorithm DIGEST_ALGO = DigestAlgorithm.MD5;

    // Instance
    @LauncherNetworkAPI
    public final long size;
    @LauncherNetworkAPI
    private final byte[] digest;


    public HashedFile(HInput input) throws IOException {
        this(input.readVarLong(), input.readBoolean() ? input.readByteArray(-DIGEST_ALGO.bytes) : null);
    }

    public HashedFile(DataInputStream input) throws IOException {
        this(input.readLong(), input.readBoolean() ? readDigest(input) : null);
    }

    private static byte[] readDigest(DataInputStream input) throws IOException {
        byte[] bytes = new byte[input.readInt()];
        input.readFully(bytes);
        return bytes;
    }


    public HashedFile(long size, byte[] digest) {
        this.size = VerifyHelper.verifyLong(size, VerifyHelper.L_NOT_NEGATIVE, "Illegal size: " + size);
        this.digest = digest == null ? null : DIGEST_ALGO.verify(digest).clone();
    }


    public HashedFile(Path file, long size, boolean digest) throws IOException {
        this(size, digest ? SecurityHelper.digest(DIGEST_ALGO, file) : null);
    }

    @Override
    public Type getType() {
        return Type.FILE;
    }


    public boolean isSame(HashedFile o) {
        return size == o.size && (digest == null || o.digest == null || Arrays.equals(digest, o.digest));
    }


    public boolean isSame(Path file, boolean digest) throws IOException {
        if (size != IOHelper.readAttributes(file).size())
            return false;
        if (!digest || this.digest == null)
            return true;

        // Create digest
        byte[] actualDigest = SecurityHelper.digest(DIGEST_ALGO, file);
        return Arrays.equals(this.digest, actualDigest);
    }


    public boolean isSameDigest(byte[] digest) {
        return this.digest == null || digest == null || Arrays.equals(this.digest, digest);
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public void write(DataOutputStream stream) throws IOException {
        stream.writeLong(size);
        if(digest == null) {
            stream.writeBoolean(false);
        } else {
            stream.writeBoolean(true);
            stream.writeInt(digest.length);
            stream.write(digest);
        }
    }

    @Override
    public void write(HOutput output) throws IOException {
        output.writeVarLong(size);
        output.writeBoolean(digest != null);
        if (digest != null)
            output.writeByteArray(digest, -DIGEST_ALGO.bytes);
    }
}
