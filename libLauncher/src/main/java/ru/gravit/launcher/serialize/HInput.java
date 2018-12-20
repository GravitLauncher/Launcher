package ru.gravit.launcher.serialize;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.utils.helper.IOHelper;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

public final class HInput implements AutoCloseable {
    @LauncherAPI
    public final InputStream stream;

    @LauncherAPI
    public HInput(byte[] bytes) {
        stream = new ByteArrayInputStream(bytes);
    }

    @LauncherAPI
    public HInput(InputStream stream) {
        this.stream = Objects.requireNonNull(stream, "stream");
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    @LauncherAPI
    public String readASCII(int maxBytes) throws IOException {
        return IOHelper.decodeASCII(readByteArray(maxBytes));
    }

    @LauncherAPI
    public BigInteger readBigInteger(int maxBytes) throws IOException {
        return new BigInteger(readByteArray(maxBytes));
    }

    @LauncherAPI
    public boolean readBoolean() throws IOException {
        int b = readUnsignedByte();
        switch (b) {
            case 0b0:
                return false;
            case 0b1:
                return true;
            default:
                throw new IOException("Invalid boolean state: " + b);
        }
    }

    @LauncherAPI
    public byte[] readByteArray(int max) throws IOException {
        byte[] bytes = new byte[readLength(max)];
        IOHelper.read(stream, bytes);
        return bytes;
    }

    @LauncherAPI
    public int readInt() throws IOException {
        return (readUnsignedByte() << 24) + (readUnsignedByte() << 16) + (readUnsignedByte() << 8) + readUnsignedByte();
    }

    @LauncherAPI
    public int readLength(int max) throws IOException {
        if (max < 0)
            return -max;
        return IOHelper.verifyLength(readVarInt(), max);
    }

    @LauncherAPI
    public long readLong() throws IOException {
        return (long) readInt() << 32 | readInt() & 0xFFFFFFFFL;
    }

    @LauncherAPI
    public short readShort() throws IOException {
        return (short) ((readUnsignedByte() << 8) + readUnsignedByte());
    }

    @LauncherAPI
    public String readString(int maxBytes) throws IOException {
        return IOHelper.decode(readByteArray(maxBytes));
    }

    @LauncherAPI
    public int readUnsignedByte() throws IOException {
        int b = stream.read();
        if (b < 0)
            throw new EOFException("readUnsignedByte");
        return b;
    }

    @LauncherAPI
    public int readUnsignedShort() throws IOException {
        return Short.toUnsignedInt(readShort());
    }

    @LauncherAPI
    public UUID readUUID() throws IOException {
        return new UUID(readLong(), readLong());
    }

    @LauncherAPI
    public int readVarInt() throws IOException {
        int shift = 0;
        int result = 0;
        while (shift < Integer.SIZE) {
            int b = readUnsignedByte();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0)
                return result;
            shift += 7;
        }
        throw new IOException("VarInt too big");
    }

    @LauncherAPI
    public long readVarLong() throws IOException {
        int shift = 0;
        long result = 0;
        while (shift < Long.SIZE) {
            int b = readUnsignedByte();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0)
                return result;
            shift += 7;
        }
        throw new IOException("VarLong too big");
    }
}
