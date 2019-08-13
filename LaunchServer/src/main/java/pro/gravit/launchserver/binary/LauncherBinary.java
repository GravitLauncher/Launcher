package pro.gravit.launchserver.binary;

import java.io.IOException;
import java.nio.file.Path;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

public abstract class LauncherBinary {
    public final LaunchServer server;
    public final Path syncBinaryFile;
    private volatile byte[] digest;
    private volatile byte[] sign;

    protected LauncherBinary(LaunchServer server, Path binaryFile) {
        this.server = server;
        syncBinaryFile = binaryFile;
    }

    public abstract void build() throws IOException;


    public final boolean exists() {
        return syncBinaryFile != null && IOHelper.isFile(syncBinaryFile);
    }


    public final byte[] getDigest() {
        return digest;
    }

    public final byte[] getSign() {
        return sign;
    }

    public void init() {
    }

    public final boolean sync() throws IOException {
        boolean exists = exists();
        digest = exists ? SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA512, IOHelper.read(syncBinaryFile)) : null;
        sign = exists ? SecurityHelper.sign(IOHelper.read(syncBinaryFile), server.privateKey) : null;

        return exists;
    }

    public static Path resolve(LaunchServer server, String ext) {
        return server.config.copyBinaries ? server.updatesDir.resolve(server.config.binaryName + ext) : server.dir.resolve(server.config.binaryName + ext);
    }
}
