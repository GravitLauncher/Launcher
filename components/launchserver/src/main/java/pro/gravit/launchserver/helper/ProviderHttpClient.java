package pro.gravit.launchserver.helper;

import pro.gravit.launcher.base.HttpHelper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** HTTP transport owned by one remote provider instance. */
public final class ProviderHttpClient implements AutoCloseable {
    private final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
        Thread thread = new Thread(r, "LaunchServer-RemoteProvider-HTTP");
        thread.setDaemon(true);
        return thread;
    });
    private final HttpClient client = HttpClient.newBuilder()
            .executor(executor)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public HttpRequest.Builder request(String baseUrl, String path, String accessToken) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return HttpRequest.newBuilder(URI.create(base + path))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + accessToken);
    }

    public URI uri(String baseUrl, String path, Map<String, String> query) {
        StringBuilder value = new StringBuilder(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl).append(path);
        boolean first = true;
        for (var entry : query.entrySet()) {
            value.append(first ? '?' : '&');
            first = false;
            value.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return URI.create(value.toString());
    }

    public HttpRequest.Builder authenticatedBuilder(String accessToken) {
        return HttpRequest.newBuilder().timeout(Duration.ofSeconds(60)).header("Authorization", "Bearer " + accessToken);
    }

    public <T, E> CompletableFuture<HttpHelper.HttpOptional<T, E>> sendAsync(
            HttpRequest request, HttpHelper.HttpErrorHandler<T, E> handler) {
        return HttpHelper.sendAsync(client, request, handler);
    }

    public HttpClient client() { return client; }

    @Override
    public void close() {
        client.shutdownNow();
        executor.shutdownNow();
    }
}
