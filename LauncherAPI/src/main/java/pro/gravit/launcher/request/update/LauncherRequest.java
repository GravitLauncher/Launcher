package pro.gravit.launcher.request.update;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherAPI;
import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.downloader.ListDownloader;
import pro.gravit.launcher.events.request.LauncherRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.websockets.RequestInterface;
import pro.gravit.launcher.request.websockets.StandartClientWebSocketService;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

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
    public static void update(LauncherRequestEvent result) throws IOException {
        List<String> args = new ArrayList<>(8);
        args.add(IOHelper.resolveJavaBin(null).toString());
        if (LogHelper.isDebugEnabled())
            args.add(JVMHelper.jvmProperty(LogHelper.DEBUG_PROPERTY, Boolean.toString(LogHelper.isDebugEnabled())));
        args.add("-jar");
        args.add(BINARY_PATH.toString());
        ProcessBuilder builder = new ProcessBuilder(args.toArray(new String[0]));
        builder.inheritIO();

        // Rewrite and start new instance
        if (result.binary != null)
            IOHelper.write(BINARY_PATH, result.binary);
        else {
            /*URLConnection connection = IOHelper.newConnection(new URL(result.url));
            connection.setDoOutput(true);
            connection.connect();
            try (OutputStream stream = connection.getOutputStream()) {
                IOHelper.transfer(BINARY_PATH, stream);
            }*/
            try {
                ListDownloader downloader = new ListDownloader();
                downloader.downloadOne(result.url, BINARY_PATH);
            } catch (Throwable e) {
                LogHelper.error(e);
            }
        }
        builder.start();

        // Kill current instance
        JVMHelper.RUNTIME.exit(255);
        throw new AssertionError("Why Launcher wasn't restarted?!");
    }

    @Override
    public LauncherRequestEvent requestDo(StandartClientWebSocketService service) throws Exception {
        LauncherRequestEvent result = (LauncherRequestEvent) service.sendRequest(this);
        if (result.needUpdate) update(result);
        return result;
    }

    @LauncherAPI
    public LauncherRequest() {
        Path launcherPath = IOHelper.getCodeSource(LauncherRequest.class);
        try {
            digest = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA512, launcherPath);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    @Override
    public String getType() {
        return "launcher";
    }
}
