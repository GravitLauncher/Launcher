package ru.gravit.launchserver.binary;

import java.io.IOException;
import java.nio.file.Path;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.launcher.serialize.signed.SignedBytesHolder;
import ru.gravit.launchserver.LaunchServer;

public abstract class LauncherBinary {
    @LauncherAPI
    protected final LaunchServer server;
    @LauncherAPI
    protected final Path binaryFile;
    protected final Path syncBinaryFile;
    private volatile SignedBytesHolder binary;

    @LauncherAPI
    protected LauncherBinary(LaunchServer server, Path binaryFile) {
        this.server = server;
        this.binaryFile = binaryFile;
        syncBinaryFile = binaryFile;
    }
    @LauncherAPI
    protected LauncherBinary(LaunchServer server, Path binaryFile, Path syncBinaryFile) {
        this.server = server;
        this.binaryFile = binaryFile;
        this.syncBinaryFile = syncBinaryFile;
    }

    @LauncherAPI
    public abstract void build() throws IOException;

    @LauncherAPI
    public final boolean exists() {
        return IOHelper.isFile(syncBinaryFile);
    }

    @LauncherAPI
    public final SignedBytesHolder getBytes() {
        return binary;
    }

    @LauncherAPI
    public final boolean sync() throws IOException {
        boolean exists = exists();
        binary = exists ? new SignedBytesHolder(IOHelper.read(syncBinaryFile), server.privateKey) : null;
        return exists;
    }
}
