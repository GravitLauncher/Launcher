package ru.gravit.launcher.request.update;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.request.LauncherRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class LauncherRequest extends Request<LauncherRequestEvent> implements RequestInterface {
    @LauncherNetworkAPI
    public byte[] digest;
    @LauncherNetworkAPI
    public int launcher_type = EXE_BINARY ? 2 : 1;
    @LauncherAPI
    public static final Path BINARY_PATH = IOHelper.getCodeSource(Launcher.class);

    @LauncherAPI
    public static final boolean EXE_BINARY = IOHelper.hasExtension(BINARY_PATH, "exe");

    @LauncherAPI
    public static void update(LauncherConfig config, LauncherRequestEvent result) throws IOException {
        List<String> args = new ArrayList<>(8);
        args.add(IOHelper.resolveJavaBin(null).toString());
        if (LogHelper.isDebugEnabled())
            args.add(JVMHelper.jvmProperty(LogHelper.DEBUG_PROPERTY, Boolean.toString(LogHelper.isDebugEnabled())));
        args.add("-jar");
        args.add(BINARY_PATH.toString());
        ProcessBuilder builder = new ProcessBuilder(args.toArray(new String[0]));
        builder.inheritIO();

        // Rewrite and start new instance
        if(result.binary != null)
            IOHelper.write(BINARY_PATH, result.binary);
        else
        {
             URLConnection connection = IOHelper.newConnection(new URL(result.url));
             connection.connect();
             try(OutputStream stream = connection.getOutputStream()) {
                 IOHelper.transfer(BINARY_PATH, stream);
             }
        }
        builder.start();

        // Kill current instance
        JVMHelper.RUNTIME.exit(255);
        throw new AssertionError("Why Launcher wasn't restarted?!");
    }

    @LauncherAPI
    public LauncherRequest() {
        this(null);
    }

    @Override
    public LauncherRequestEvent requestWebSockets() throws Exception
    {
        LauncherRequestEvent result = (LauncherRequestEvent) LegacyRequestBridge.sendRequest(this);
        if(result.needUpdate) update(config, result);
        return result;
    }

    @LauncherAPI
    public LauncherRequest(LauncherConfig config) {
        super(config);
        Path launcherPath = IOHelper.getCodeSource(LauncherRequest.class);
        try {
            digest = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA512, launcherPath);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    @Override
    public Integer getLegacyType() {
        return RequestType.LAUNCHER.getNumber();
    }

    @Override
    protected LauncherRequestEvent requestDo(HInput input, HOutput output) throws Exception {
        output.writeBoolean(EXE_BINARY);
        output.writeByteArray(digest, 0);
        output.flush();
        readError(input);

        // Verify launcher sign
        boolean shouldUpdate = input.readBoolean();
        if (shouldUpdate) {
            byte[] binary = input.readByteArray(0);
            LauncherRequestEvent result = new LauncherRequestEvent(binary, digest);
            update(Launcher.getConfig(), result);
        }

        // Return request result
        return new LauncherRequestEvent(null, digest);
    }

    @Override
    public String getType() {
        return "launcher";
    }
}
