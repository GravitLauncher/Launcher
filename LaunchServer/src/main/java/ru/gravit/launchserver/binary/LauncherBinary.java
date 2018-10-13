package ru.gravit.launchserver.binary;

import java.io.IOException;
import java.nio.file.Path;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.launcher.serialize.signed.SignedBytesHolder;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.SecurityHelper;

public abstract class LauncherBinary {

    protected final LaunchServer server;

    protected final Path binaryFile;
    protected final Path syncBinaryFile;
    private volatile SignedBytesHolder binary;
    private volatile byte[] hash;


    protected LauncherBinary(LaunchServer server, Path binaryFile) {
        this.server = server;
        this.binaryFile = binaryFile;
        syncBinaryFile = binaryFile;
    }


    protected LauncherBinary(LaunchServer server, Path binaryFile, Path syncBinaryFile) {
        this.server = server;
        this.binaryFile = binaryFile;
        this.syncBinaryFile = syncBinaryFile;
    }


    public abstract void build() throws IOException;


    public final boolean exists() {
        return IOHelper.isFile(syncBinaryFile);
    }


    public final SignedBytesHolder getBytes() {
        return binary;
    }

    public final byte[] getHash() {
        return hash;
    }


    public final boolean sync() throws IOException {
        boolean exists = exists();
        binary = exists ? new SignedBytesHolder(IOHelper.read(syncBinaryFile), server.privateKey) : null;
        hash = exists ? SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA512,IOHelper.newInput(syncBinaryFile)) : null;
        return exists;
    }
}
