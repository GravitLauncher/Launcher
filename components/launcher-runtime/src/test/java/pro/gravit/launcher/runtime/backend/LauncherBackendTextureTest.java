package pro.gravit.launcher.runtime.backend;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import pro.gravit.launcher.core.api.model.Texture;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LauncherBackendTextureTest {
    @Test
    void rejectsNonHttpTextureUrls() {
        assertThrows(CompletionException.class, () -> new LauncherBackendImpl().fetchTexture(texture("file:///etc/passwd")).join());
    }

    @Test
    void fetchesTextureFromLocalHttpServer() throws Exception {
        byte[] expected = "texture bytes".getBytes(StandardCharsets.UTF_8);
        try (TestServer server = new TestServer(exchange -> {
            exchange.sendResponseHeaders(200, expected.length);
            try (var output = exchange.getResponseBody()) { output.write(expected); }
        })) {
            assertArrayEquals(expected, new LauncherBackendImpl().fetchTexture(texture(server.url("/texture"))).get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void reportsHttpErrors() throws Exception {
        try (TestServer server = new TestServer(exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        })) {
            CompletionException error = assertThrows(CompletionException.class,
                    () -> new LauncherBackendImpl().fetchTexture(texture(server.url("/error"))).join());
            assertInstanceOf(IOException.class, error.getCause());
            assertTrue(error.getCause().getMessage().contains("503"));
        }
    }

    @Test
    void rejectsBodiesLargerThanEightMiB() throws Exception {
        byte[] oversized = new byte[8 * 1024 * 1024 + 1];
        try (TestServer server = new TestServer(exchange -> {
            exchange.sendResponseHeaders(200, oversized.length);
            try (var output = exchange.getResponseBody()) { output.write(oversized); }
        })) {
            CompletionException error = assertThrows(CompletionException.class,
                    () -> new LauncherBackendImpl().fetchTexture(texture(server.url("/large"))).join());
            assertTrue(error.getCause().getMessage().contains("8 MiB"));
        }
    }

    @Test
    void cancellationCompletesAndClosesAnOpenResponseBody() throws Exception {
        CountDownLatch responseSent = new CountDownLatch(1);
        CountDownLatch finishResponse = new CountDownLatch(1);
        try (TestServer server = new TestServer(exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (var output = exchange.getResponseBody()) {
                output.write(1);
                output.flush();
                responseSent.countDown();
                try { finishResponse.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            } catch (IOException ignored) {
                // The client cancellation can close the response stream.
            } finally {
                exchange.close();
            }
        })) {
            CompletableFuture<byte[]> future = new LauncherBackendImpl().fetchTexture(texture(server.url("/slow")));
            assertTrue(responseSent.await(5, TimeUnit.SECONDS));
            assertTrue(future.cancel(true));
            assertTrue(future.isCancelled());
            finishResponse.countDown();
        } finally {
            finishResponse.countDown();
        }
    }

    private static Texture texture(String url) {
        return new Texture() {
            @Override public String getUrl() { return url; }
            @Override public String getHash() { return ""; }
            @Override public Map<String, String> getMetadata() { return Map.of(); }
        };
    }

    private static final class TestServer implements AutoCloseable {
        private final HttpServer server;

        TestServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", handler);
            server.start();
        }

        String url(String path) {
            return "http://127.0.0.1:" + server.getAddress().getPort() + path;
        }

        @Override public void close() {
            server.stop(0);
        }
    }
}
