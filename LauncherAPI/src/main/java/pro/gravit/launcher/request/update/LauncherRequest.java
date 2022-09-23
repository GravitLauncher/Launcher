package pro.gravit.launcher.request.update;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.LauncherRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestService;
import pro.gravit.launcher.request.websockets.WebSocketRequest;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Path;

public final class LauncherRequest extends Request<LauncherRequestEvent> implements WebSocketRequest {
    public static final Path BINARY_PATH = IOHelper.getCodeSource(Launcher.class);
    public static final boolean EXE_BINARY = IOHelper.hasExtension(BINARY_PATH, "exe");
    @LauncherNetworkAPI
    public final String secureHash;
    @LauncherNetworkAPI
    public final String secureSalt;
    @LauncherNetworkAPI
    public byte[] digest;
    @LauncherNetworkAPI
    public int launcher_type = EXE_BINARY ? 2 : 1;


    public LauncherRequest() {
        Path launcherPath = IOHelper.getCodeSource(LauncherRequest.class);
        try {
            digest = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA512, launcherPath);
        } catch (IOException e) {
            LogHelper.error(e);
        }
        secureHash = Launcher.getConfig().secureCheckHash;
        secureSalt = Launcher.getConfig().secureCheckSalt;
    }

    @Override
    public LauncherRequestEvent requestDo(RequestService service) throws Exception {
        return super.request(service);
    }

    @Override
    public String getType() {
        return "launcher";
    }
}
