package pro.gravit.utils;

import pro.gravit.launcher.AsyncDownloader;
import pro.gravit.launcher.LauncherInject;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Downloader {
    @LauncherInject("launcher.certificatePinning")
    private static boolean isCertificatePinning;
    @LauncherInject("launcher.noHttp2")
    private static boolean isNoHttp2;

    public interface DownloadCallback {
        void apply(long fullDiff);

        void onComplete(Path path);
    }

    public static CompletableFuture<Void> downloadList(List<AsyncDownloader.SizedFile> files, String baseURL, Path targetDir, DownloadCallback callback, ExecutorService executor, int threads) throws Exception {
        boolean closeExecutor = false;
        if (executor == null) {
            executor = Executors.newWorkStealingPool(Math.min(3, threads));
            closeExecutor = true;
        }
        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(isNoHttp2 ? HttpClient.Version.HTTP_1_1 : HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .executor(executor);
        if (isCertificatePinning) {
            try {
                builder.sslContext(AsyncDownloader.makeSSLContext());
            } catch (Exception e) {
                throw new SecurityException(e);
            }
        }
        CompletableFuture<Void> future = downloadList(builder.build(), files, baseURL, targetDir, callback, executor, threads);
        if (closeExecutor) {
            ExecutorService finalExecutor = executor;
            future = future.thenAccept(e -> {
                finalExecutor.shutdownNow();
            });
        }
        return future;
    }

    private static class ConsumerObject {
        Consumer<HttpResponse<Path>> next = null;
    }

    public static CompletableFuture<Void> downloadList(HttpClient client, List<AsyncDownloader.SizedFile> files, String baseURL, Path targetDir, DownloadCallback callback, ExecutorService executor, int threads) throws Exception {
        // URI scheme
        URI baseUri = new URI(baseURL);
        Collections.shuffle(files);
        Queue<AsyncDownloader.SizedFile> queue = new ConcurrentLinkedDeque<>(files);
        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicInteger currentThreads = new AtomicInteger(threads);
        ConsumerObject consumerObject = new ConsumerObject();
        Consumer<HttpResponse<Path>> next = e -> {
            if (callback != null && e != null) {
                callback.onComplete(e.body());
            }
            AsyncDownloader.SizedFile file = queue.poll();
            if (file == null) {
                if (currentThreads.decrementAndGet() == 0)
                    future.complete(null);
                return;
            }
            try {
                sendAsync(client, file, baseUri, targetDir, callback).thenAccept(consumerObject.next);
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        };
        consumerObject.next = next;
        for (int i = 0; i < threads; ++i) {
            next.accept(null);
        }
        return future;
    }

    private static CompletableFuture<HttpResponse<Path>> sendAsync(HttpClient client, AsyncDownloader.SizedFile file, URI baseUri, Path targetDir, DownloadCallback callback) throws Exception {
        return client.sendAsync(makeHttpRequest(baseUri, file.urlPath), makeBodyHandler(targetDir.resolve(file.filePath), callback));
    }

    private static HttpRequest makeHttpRequest(URI baseUri, String filePath) throws URISyntaxException {
        String scheme = baseUri.getScheme();
        String host = baseUri.getHost();
        int port = baseUri.getPort();
        if (port != -1)
            host = host + ":" + port;
        String path = baseUri.getPath();
        return HttpRequest.newBuilder()
                .GET()
                .uri(new URI(scheme, host, path + filePath, "", ""))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36")
                .build();
    }

    private static HttpResponse.BodyHandler<Path> makeBodyHandler(Path file, DownloadCallback callback) {
        return new ProgressTrackingBodyHandler<>(HttpResponse.BodyHandlers.ofFile(file), callback);
    }

    public static class ProgressTrackingBodyHandler<T> implements HttpResponse.BodyHandler<T> {
        private final HttpResponse.BodyHandler<T> delegate;
        private final DownloadCallback callback;

        public ProgressTrackingBodyHandler(HttpResponse.BodyHandler<T> delegate, DownloadCallback callback) {
            this.delegate = delegate;
            this.callback = callback;
        }

        @Override
        public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
            return delegate.apply(responseInfo);
        }

        private class ProgressTrackingBodySubscriber implements HttpResponse.BodySubscriber<T> {
            private final HttpResponse.BodySubscriber<T> delegate;

            public ProgressTrackingBodySubscriber(HttpResponse.BodySubscriber<T> delegate) {
                this.delegate = delegate;
            }

            @Override
            public CompletionStage<T> getBody() {
                return delegate.getBody();
            }

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                delegate.onSubscribe(subscription);
            }

            @Override
            public void onNext(List<ByteBuffer> byteBuffers) {
                delegate.onNext(byteBuffers);
                long diff = 0;
                for (ByteBuffer buffer : byteBuffers) {
                    diff += buffer.remaining();
                }
                if (callback != null) callback.apply(diff);
            }

            @Override
            public void onError(Throwable throwable) {
                delegate.onError(throwable);
            }

            @Override
            public void onComplete() {
                delegate.onComplete();
            }
        }
    }
}
