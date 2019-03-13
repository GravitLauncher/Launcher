package ru.gravit.launcher.request.update;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.request.update.LegacyLauncherRequest.Result;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LegacyLauncherRequest extends Request<Result> {
    public static final class Result {
        @LauncherAPI
        public final List<ClientProfile> profiles;
        private final byte[] binary;
        private final byte[] sign;

        public Result(byte[] binary, byte[] sign, List<ClientProfile> profiles) {
            this.binary = binary == null ? null : binary.clone();
            this.sign = sign.clone();
            this.profiles = Collections.unmodifiableList(profiles);
        }

        @LauncherAPI
        public byte[] getBinary() {
            return binary == null ? null : binary.clone();
        }

        @LauncherAPI
        public byte[] getSign() {
            return sign.clone();
        }
    }

    @LauncherAPI
    public static final Path BINARY_PATH = IOHelper.getCodeSource(Launcher.class);

    @LauncherAPI
    public static final boolean EXE_BINARY = IOHelper.hasExtension(BINARY_PATH, "exe");

    @LauncherAPI
    public static void update(LauncherConfig config, Result result) throws SignatureException, IOException {
        SecurityHelper.verifySign(result.binary, result.sign, config.publicKey);

        // Prepare process builder to start new instance (java -jar works for Launch4J's EXE too)
        List<String> args = new ArrayList<>(8);
        args.add(IOHelper.resolveJavaBin(null).toString());
        if (LogHelper.isDebugEnabled())
            args.add(JVMHelper.jvmProperty(LogHelper.DEBUG_PROPERTY, Boolean.toString(LogHelper.isDebugEnabled())));
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
    public LegacyLauncherRequest() {
        this(null);
    }

    @LauncherAPI
    public LegacyLauncherRequest(LauncherConfig config) {
        super(config);
    }

    @Override
    public Integer getLegacyType() {
        return RequestType.LEGACYLAUNCHER.getNumber();
    }

    @Override
    protected Result requestDo(HInput input, HOutput output) throws Exception {
        output.writeBoolean(EXE_BINARY);
        output.flush();
        readError(input);

        // Verify launcher sign
        RSAPublicKey publicKey = config.publicKey;
        byte[] sign = input.readByteArray(-SecurityHelper.RSA_KEY_LENGTH);
        boolean shouldUpdate = !SecurityHelper.isValidSign(BINARY_PATH, sign, publicKey);

        // Update launcher if need
        output.writeBoolean(shouldUpdate);
        output.flush();
        if (shouldUpdate) {
            byte[] binary = input.readByteArray(0);
            SecurityHelper.verifySign(binary, sign, config.publicKey);
            return new Result(binary, sign, Collections.emptyList());
        }

        // Read clients profiles list
        int count = input.readLength(0);
        List<ClientProfile> profiles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String prof = input.readString(0);
            profiles.add(Launcher.gson.fromJson(prof, ClientProfile.class));
        }

        // Return request result
        return new Result(null, sign, profiles);
    }
}
