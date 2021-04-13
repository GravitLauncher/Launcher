package pro.gravit.launchserver.binary;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Path;

public abstract class LauncherBinary extends BinaryPipeline {
    public final LaunchServer server;
    public final Path syncBinaryFile;
    private volatile byte[] digest;

    protected LauncherBinary(LaunchServer server, Path binaryFile, String nameFormat) {
        super(server.tmpDir.resolve("build"), nameFormat);
        this.server = server;
        syncBinaryFile = binaryFile;
    }

    public static Path resolve(LaunchServer server, String ext) {
        return server.config.copyBinaries ? server.updatesDir.resolve(server.config.binaryName + ext) : server.dir.resolve(server.config.binaryName + ext);
    }

    public void build() throws IOException {
        build(syncBinaryFile, server.config.launcher.deleteTempFiles);
    }

    public final boolean exists() {
        return syncBinaryFile != null && IOHelper.isFile(syncBinaryFile);
    }

    public final byte[] getDigest() {
        return digest;
    }

    public void init() {
    }

    public final boolean sync() throws IOException {
        boolean exists = exists();
        digest = exists ? SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA512, IOHelper.read(syncBinaryFile)) : null;

        return exists;
    }
}
