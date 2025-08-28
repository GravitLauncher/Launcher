package pro.gravit.launcher.base.request;

import pro.gravit.launcher.base.request.auth.GetAvailabilityAuthRequest;
import pro.gravit.launcher.base.request.update.LauncherRequest;
import pro.gravit.launcher.core.api.features.CoreFeatureAPI;
import pro.gravit.launcher.core.api.method.AuthMethod;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RequestCoreFeatureAPIImpl implements CoreFeatureAPI {
    private final RequestService request;

    public RequestCoreFeatureAPIImpl(RequestService request) {
        this.request = request;
    }



    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public CompletableFuture<List<AuthMethod>> getAuthMethods() {
        return request.request(new GetAvailabilityAuthRequest()).thenApply(response -> (List) response.list);
    }

    @Override
    public CompletableFuture<LauncherUpdateInfo> checkUpdates() {
        return request.request(new LauncherRequest()).thenApply(response -> new LauncherUpdateInfo(response.url,
                "Unknown", response.needUpdate, response.needUpdate));
    }
}
