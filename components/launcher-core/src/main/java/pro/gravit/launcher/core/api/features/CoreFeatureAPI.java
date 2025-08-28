package pro.gravit.launcher.core.api.features;

import pro.gravit.launcher.core.api.method.AuthMethod;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CoreFeatureAPI {
    CompletableFuture<List<AuthMethod>> getAuthMethods();
    CompletableFuture<LauncherUpdateInfo> checkUpdates();

    record LauncherUpdateInfo(String url, String version, boolean available, boolean required) {}
}
