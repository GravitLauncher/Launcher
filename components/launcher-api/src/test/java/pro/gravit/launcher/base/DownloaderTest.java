package pro.gravit.launcher.base;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DownloaderTest {
    @TempDir Path dir;
    private HttpServer server;
    private String baseUrl;
    private Downloader downloader;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/good", exchange -> {
            try (exchange) {
                exchange.sendResponseHeaders(200, 3);
                exchange.getResponseBody().write(new byte[]{1, 2, 3});
            }
        });
        server.createContext("/bad", exchange -> {
            try (exchange) { exchange.sendResponseHeaders(404, -1); }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void close() {
        if (downloader != null) downloader.cancel();
        if (server != null) server.stop(0);
    }

    @Test
    void partialBatchFailureDoesNotReportSuccess() throws Exception {
        downloader = Downloader.downloadList(new ArrayList<>(List.of(
                new Downloader.SizedFile("good", "good.bin", 3),
                new Downloader.SizedFile("bad", "bad.bin", 3))), baseUrl + "/", dir, null, null, 2);
        assertThrows(ExecutionException.class, () -> downloader.getFuture().get(5, TimeUnit.SECONDS));
        assertFalse(Files.exists(dir.resolve("bad.bin")));
    }

    @Test
    void sizeMismatchPreservesExistingDestination() throws Exception {
        Path destination = dir.resolve("existing.bin");
        Files.writeString(destination, "original");
        downloader = Downloader.downloadList(new ArrayList<>(List.of(
                new Downloader.SizedFile("good", "existing.bin", 100))), baseUrl + "/", dir, null, null, 1);
        assertThrows(ExecutionException.class, () -> downloader.getFuture().get(5, TimeUnit.SECONDS));
        assertEquals("original", Files.readString(destination));
        try (var files = Files.list(dir)) {
            assertEquals(List.of(destination), files.toList());
        }
    }
}
