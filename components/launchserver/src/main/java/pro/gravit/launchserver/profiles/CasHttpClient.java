package pro.gravit.launchserver.profiles;

import pro.gravit.launcher.base.HttpHelper;
import pro.gravit.launchserver.helper.ProviderHttpClient;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** CAS endpoint paths and authorization layered on the provider-owned HTTP transport. */
final class CasHttpClient {
    private final ProviderHttpClient client;
    private final String baseUrl;
    private final String accessToken;

    CasHttpClient(ProviderHttpClient client, String baseUrl, String accessToken) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.accessToken = accessToken;
    }

    HttpRequest.Builder request(String path) {
        return client.request(baseUrl, path, accessToken);
    }

    HttpRequest.Builder authorizedBuilder() {
        return client.authenticatedBuilder(accessToken);
    }

    URI uri(String path, Map<String, String> query) {
        return client.uri(baseUrl, path, query);
    }

    <T, E> CompletableFuture<HttpHelper.HttpOptional<T, E>> sendAsync(
            HttpRequest request, HttpHelper.HttpErrorHandler<T, E> handler) {
        return client.sendAsync(request, handler);
    }
}
