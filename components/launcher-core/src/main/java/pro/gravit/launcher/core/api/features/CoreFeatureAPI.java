package pro.gravit.launcher.core.api.features;

import pro.gravit.launcher.core.api.method.AuthMethod;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CoreFeatureAPI {
    CompletableFuture<List<AuthMethod>> getAuthMethods();
    CompletableFuture<LauncherUpdateInfo> checkUpdates();

    record LauncherUpdateInfo(String url, String version, boolean available, boolean required) {}

    enum UpdateVariant {
        JAR,
        EXE_WINDOWS_X86_64, EXE_WINDOWS_X86, EXE_WINDOWS_ARM64,
        LINUX_X86, LINUX_X86_64, LINUX_ARM64, LINUX_ARM32,
        MACOS_X86_64, MACOS_ARM64
    }
}
