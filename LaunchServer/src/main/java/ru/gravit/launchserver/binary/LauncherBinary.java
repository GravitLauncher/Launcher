package ru.gravit.launchserver.binary;

import ru.gravit.launcher.serialize.signed.DigestBytesHolder;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Path;

public abstract class LauncherBinary {
    public final LaunchServer server;
    public final Path syncBinaryFile;
    private volatile DigestBytesHolder binary;
    private volatile byte[] sign;

    protected LauncherBinary(LaunchServer server, Path binaryFile) {
        this.server = server;
        syncBinaryFile = binaryFile;
    }

    public abstract void build() throws IOException;


    public final boolean exists() {
        return syncBinaryFile != null && IOHelper.isFile(syncBinaryFile);
    }


    public final DigestBytesHolder getBytes() {
        return binary;
    }

    public final byte[] getSign() {
        return sign;
    }

    public void init() {
    }

    public final boolean sync() throws IOException {
        boolean exists = exists();
        binary = exists ? new DigestBytesHolder(IOHelper.read(syncBinaryFile), SecurityHelper.DigestAlgorithm.SHA512) : null;
        sign = exists ? SecurityHelper.sign(IOHelper.read(syncBinaryFile), server.privateKey) : null;

        return exists;
    }
    
    public static Path resolve(LaunchServer server, String ext) {
    	return server.config.copyBinaries ? server.updatesDir.resolve(server.config.binaryName + ext) : server.dir.resolve(server.config.binaryName + ext);
    }
}
