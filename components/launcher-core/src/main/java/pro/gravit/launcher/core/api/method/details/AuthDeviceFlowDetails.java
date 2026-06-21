package pro.gravit.launcher.core.api.method.details;

import pro.gravit.launcher.core.api.method.AuthMethodDetails;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public record AuthDeviceFlowDetails(String url, Supplier<CompletableFuture<AuthDeviceFlowDetailsData>> dataSupplier, boolean externalBrowserSupport) implements AuthMethodDetails {
    public record AuthDeviceFlowDetailsData(String deviceCode, String userCode) {

    }
}
