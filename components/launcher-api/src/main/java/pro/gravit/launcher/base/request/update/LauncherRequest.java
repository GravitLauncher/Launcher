package pro.gravit.launcher.base.request.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.base.events.request.LauncherRequestEvent;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.RequestService;
import pro.gravit.launcher.base.request.websockets.WebSocketRequest;
import pro.gravit.launcher.core.api.features.CoreFeatureAPI;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Path;

public final class LauncherRequest extends Request<LauncherRequestEvent> implements WebSocketRequest {

    private static final Logger logger =
            LoggerFactory.getLogger(LauncherRequest.class);

    public static final Path BINARY_PATH = IOHelper.getCodeSource(Launcher.class);
    public static final boolean JAR_BINARY = IOHelper.hasExtension(BINARY_PATH, "jar");
    @LauncherNetworkAPI
    public final String secureHash;
    @LauncherNetworkAPI
    public final String secureSalt;
    @LauncherNetworkAPI
    public byte[] digest;
    @LauncherNetworkAPI
    public CoreFeatureAPI.UpdateVariant variant;


    public LauncherRequest() {
        Path launcherPath = IOHelper.getCodeSource(LauncherRequest.class);
        try {
            digest = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA512, launcherPath);
        } catch (IOException e) {
            logger.error("", e);
        }
        secureHash = Launcher.getConfig().secureCheckHash;
        secureSalt = Launcher.getConfig().secureCheckSalt;
        variant = getUpdateVariant();
    }

    public static CoreFeatureAPI.UpdateVariant getUpdateVariant() {
        if(JAR_BINARY) {
            return CoreFeatureAPI.UpdateVariant.JAR;
        }
        switch (JVMHelper.OS_TYPE) {
            case MUSTDIE -> {
                switch (JVMHelper.ARCH_TYPE) {
                    case X86 -> {
                        return CoreFeatureAPI.UpdateVariant.EXE_WINDOWS_X86;
                    }
                    case X86_64 -> {
                        return CoreFeatureAPI.UpdateVariant.EXE_WINDOWS_X86_64;
                    }
                    case ARM64 -> {
                        return CoreFeatureAPI.UpdateVariant.EXE_WINDOWS_ARM64;
                    }
                    default -> {
                        return CoreFeatureAPI.UpdateVariant.JAR; // Unsupported
                    }
                }
            }
            case LINUX -> {
                switch (JVMHelper.ARCH_TYPE) {
                    case X86 -> {
                        return CoreFeatureAPI.UpdateVariant.LINUX_X86;
                    }
                    case X86_64 -> {
                        return CoreFeatureAPI.UpdateVariant.LINUX_X86_64;
                    }
                    case ARM64 -> {
                        return CoreFeatureAPI.UpdateVariant.LINUX_ARM64;
                    }
                    case ARM32 -> {
                        return CoreFeatureAPI.UpdateVariant.LINUX_ARM32;
                    }
                    default -> {
                        return CoreFeatureAPI.UpdateVariant.JAR; // Unsupported
                    }
                }
            }
            case MACOSX -> {
                switch (JVMHelper.ARCH_TYPE) {
                    case X86_64 -> {
                        return CoreFeatureAPI.UpdateVariant.MACOS_X86_64;
                    }
                    case ARM64 -> {
                        return CoreFeatureAPI.UpdateVariant.MACOS_ARM64;
                    }
                    default -> {
                        return CoreFeatureAPI.UpdateVariant.JAR; // Unsupported
                    }
                }
            }
        }
        return CoreFeatureAPI.UpdateVariant.JAR;
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