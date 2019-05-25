package ru.gravit.launcher.serialize;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.utils.helper.IOHelper;

import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

public final class HOutput implements AutoCloseable, Flushable {
    @LauncherAPI
    public final OutputStream stream;

    @LauncherAPI
    public HOutput(OutputStream stream) {
        this.stream = Objects.requireNonNull(stream, "stream");
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    @Override
    public void flush() throws IOException {
        stream.flush();
    }

    @LauncherAPI
    public void writeASCII(String s, int maxBytes) throws IOException {
        writeByteArray(IOHelper.encodeASCII(s), maxBytes);
    }

    @LauncherAPI
    public void writeBigInteger(BigInteger bi, int max) throws IOException {
        writeByteArray(bi.toByteArray(), max);
    }

    @LauncherAPI
    public void writeBoolean(boolean b) throws IOException {
        writeUnsignedByte(b ? 0b1 : 0b0);
    }

    @LauncherAPI
    public void writeByteArray(byte[] bytes, int max) throws IOException {
        writeLength(bytes.length, max);
        stream.write(bytes);
    }

    @LauncherAPI
    public void writeInt(int i) throws IOException {
        writeUnsignedByte(i >>> 24 & 0xFF);
        writeUnsignedByte(i >>> 16 & 0xFF);
        writeUnsignedByte(i >>> 8 & 0xFF);
        writeUnsignedByte(i & 0xFF);
    }

    @LauncherAPI
    public void writeLength(int length, int max) throws IOException {
        IOHelper.verifyLength(length, max);
        if (max >= 0)
            writeVarInt(length);
    }

    @LauncherAPI
    public void writeLong(long l) throws IOException {
        writeInt((int) (l >> 32));
        writeInt((int) l);
    }

    @LauncherAPI
    public void writeShort(short s) throws IOException {
        writeUnsignedByte(s >>> 8 & 0xFF);
        writeUnsignedByte(s & 0xFF);
    }

    @LauncherAPI
    public void writeString(String s, int maxBytes) throws IOException {
        writeByteArray(IOHelper.encode(s), maxBytes);
    }

    @LauncherAPI
    public void writeUnsignedByte(int b) throws IOException {
        stream.write(b);
    }

    @LauncherAPI
    public void writeUUID(UUID uuid) throws IOException {
        writeLong(uuid.getMostSignificantBits());
        writeLong(uuid.getLeastSignificantBits());
    }

    @LauncherAPI
    public void writeVarInt(int i) throws IOException {
        while ((i & ~0x7FL) != 0) {
            writeUnsignedByte(i & 0x7F | 0x80);
            i >>>= 7;
        }
        writeUnsignedByte(i);
    }

    @LauncherAPI
    public void writeVarLong(long l) throws IOException {
        while ((l & ~0x7FL) != 0) {
            writeUnsignedByte((int) l & 0x7F | 0x80);
            l >>>= 7;
        }
        writeUnsignedByte((int) l);
    }
}
