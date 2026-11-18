package pro.gravit.launchserver.helper;

import pro.gravit.launcher.base.HttpHelper;
import pro.gravit.launcher.base.request.RequestException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** HTTP transport owned by one remote provider instance. */
public final class ProviderHttpClient implements AutoCloseable {
    private static final int MAX_ERROR_BODY_BYTES = 8192;

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
        return HttpHelper.sendAsync(client, request, response -> {
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                String token = request.headers().firstValue("Authorization").orElse("");
                if (token.startsWith("Bearer ")) token = token.substring("Bearer ".length());
                throw new RequestException(describeFailure(response, token));
            }
            return handler.apply(response);
        });
    }

    public static String describeFailure(HttpResponse<InputStream> response, String... secrets) {
        URI uri = response.uri();
        String address = uri.getScheme() + "://" + uri.getHost()
                + (uri.getPort() == -1 ? "" : ":" + uri.getPort()) + uri.getRawPath();
        String summary = "HTTP %d from %s %s".formatted(response.statusCode(), response.request().method(), address);
        byte[] bytes;
        try {
            bytes = response.body().readNBytes(MAX_ERROR_BODY_BYTES + 1);
        } catch (IOException e) {
            return summary + " (could not read backend response)";
        }
        if (bytes.length > MAX_ERROR_BODY_BYTES) {
            return summary + " (backend response exceeds " + MAX_ERROR_BODY_BYTES + " bytes)";
        }

        String body = new String(bytes, StandardCharsets.UTF_8);
        for (String secret : secrets) {
            if (secret == null || secret.isEmpty()) continue;
            // Base64 padding may be omitted or escaped by a JSON response.
            String unpadded = secret.replaceFirst("=+$", "");
            if (unpadded.isEmpty()) continue;
            body = body.replace(secret, "[redacted]")
                    .replace(unpadded, "[redacted]")
                    .replace(unpadded.replace("/", "\\/"), "[redacted]");
        }
        body = body.replaceAll("[\\p{Cntrl}\\p{Zl}\\p{Zp}]+", " ").strip();
        return body.isEmpty() ? summary : summary + " — backend response: " + body;
    }

    public HttpClient client() { return client; }

    @Override
    public void close() {
        client.shutdownNow();
        executor.shutdownNow();
    }
}
