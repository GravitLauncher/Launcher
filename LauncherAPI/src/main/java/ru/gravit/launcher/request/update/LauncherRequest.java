package ru.gravit.launcher.request.update;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

public final class LauncherRequest extends Request<LauncherRequest.Result> {
    public static final class Result {
        private final byte[] binary;
        private final byte[] digest;

        public Result(byte[] binary, byte[] sign) {
            this.binary = binary == null ? null : binary.clone();
            this.digest = sign.clone();
        }

        @LauncherAPI
        public byte[] getBinary() {
            return binary == null ? null : binary.clone();
        }

        @LauncherAPI
        public byte[] getDigest() {
            return digest.clone();
        }
    }

    @LauncherAPI
    public static final Path BINARY_PATH = IOHelper.getCodeSource(Launcher.class);

    @LauncherAPI
    public static final boolean EXE_BINARY = IOHelper.hasExtension(BINARY_PATH, "exe");

    @LauncherAPI
    public static void update(LauncherConfig config, Result result) throws SignatureException, IOException {
        List<String> args = new ArrayList<>(8);
        args.add(IOHelper.resolveJavaBin(null).toString());
        if (LogHelper.isDebugEnabled())
            args.add(JVMHelper.jvmProperty(LogHelper.DEBUG_PROPERTY, Boolean.toString(LogHelper.isDebugEnabled())));
        if (LauncherConfig.ADDRESS_OVERRIDE != null)
            args.add(JVMHelper.jvmProperty(LauncherConfig.ADDRESS_OVERRIDE_PROPERTY, LauncherConfig.ADDRESS_OVERRIDE));
        args.add("-jar");
        args.add(BINARY_PATH.toString());
        ProcessBuilder builder = new ProcessBuilder(args.toArray(new String[0]));
        builder.inheritIO();

        // Rewrite and start new instance
        IOHelper.write(BINARY_PATH, result.binary);
        builder.start();

        // Kill current instance
        JVMHelper.RUNTIME.exit(255);
        throw new AssertionError("Why Launcher wasn't restarted?!");
    }

    @LauncherAPI
    public LauncherRequest() {
        this(null);
    }

    @LauncherAPI
    public LauncherRequest(LauncherConfig config) {
        super(config);
    }

    @Override
    public Integer getType() {
        return RequestType.LAUNCHER.getNumber();
    }

    @Override
    @SuppressWarnings("CallToSystemExit")
    protected Result requestDo(HInput input, HOutput output) throws Exception {
        Path launcherPath = IOHelper.getCodeSource(LauncherRequest.class);
        byte[] digest = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA512,launcherPath);
        output.writeBoolean(EXE_BINARY);
        output.writeByteArray(digest,0);
        output.flush();
        readError(input);

        // Verify launcher sign
        boolean shouldUpdate = input.readBoolean();
        if (shouldUpdate) {
            byte[] binary = input.readByteArray(0);
            Result result = new Result(binary, digest);
            update(Launcher.getConfig(),result);
        }

        // Return request result
        return new Result(null, digest);
    }
}
