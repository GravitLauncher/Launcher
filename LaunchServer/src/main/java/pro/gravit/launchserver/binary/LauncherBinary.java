package pro.gravit.launchserver.binary;

import java.io.IOException;
import java.nio.file.Path;

import pro.gravit.launcher.serialize.signed.DigestBytesHolder;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

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
        CommonHelper.removeExc(sign);
        if (binary != null) {
        	CommonHelper.removeExc(binary.getBytes());
        	CommonHelper.removeExc(binary.getDigest());
        }
        binary = exists ? new DigestBytesHolder(IOHelper.read(syncBinaryFile), SecurityHelper.DigestAlgorithm.SHA512) : null;
        sign = exists ? SecurityHelper.sign(IOHelper.read(syncBinaryFile), server.privateKey) : null;
    	CommonHelper.addExc(sign);
    	if (binary != null) {
    		CommonHelper.addExc(binary.getBytes());
    		CommonHelper.addExc(binary.getDigest());
    	}
    	return exists;
    }

    public static Path resolve(LaunchServer server, String ext) {
        return server.config.copyBinaries ? server.updatesDir.resolve(server.config.binaryName + ext) : server.dir.resolve(server.config.binaryName + ext);
    }
}
