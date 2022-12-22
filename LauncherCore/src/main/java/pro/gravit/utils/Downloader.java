package pro.gravit.utils;

import pro.gravit.launcher.AsyncDownloader;
import pro.gravit.launcher.LauncherInject;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedList;
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
    protected final HttpClient client;
    protected final ExecutorService executor;
    protected final LinkedList<DownloadTask> tasks = new LinkedList<>();
    protected CompletableFuture<Void> future;
    protected Downloader(HttpClient client, ExecutorService executor) {
        this.client = client;
        this.executor = executor;
    }

    public static Downloader downloadList(List<AsyncDownloader.SizedFile> files, String baseURL, Path targetDir, DownloadCallback callback, ExecutorService executor, int threads) throws Exception {
        boolean closeExecutor = false;
        LogHelper.info("Download with Java 11+ HttpClient");
        if (executor == null) {
            executor = Executors.newWorkStealingPool(Math.min(3, threads));
            closeExecutor = true;
        }
        Downloader downloader = newDownloader(executor);
        downloader.future = downloader.downloadFiles(files, baseURL, targetDir, callback, executor, threads);
        if (closeExecutor) {
            ExecutorService finalExecutor = executor;
            downloader.future = downloader.future.thenAccept(e -> finalExecutor.shutdownNow());
        }
        return downloader;
    }

    public static Downloader newDownloader(ExecutorService executor) {
        if (executor == null) {
            throw new NullPointerException();
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
        HttpClient client = builder.build();
        return new Downloader(client, executor);
    }

    public void cancel() {
        for (DownloadTask task : tasks) {
            if (!task.isCompleted()) {
                task.cancel();
            }
        }
        tasks.clear();
        executor.shutdownNow();
    }

    public boolean isCanceled() {
        return executor.isTerminated();
    }

    public CompletableFuture<Void> getFuture() {
        return future;
    }

    public CompletableFuture<Void> downloadFiles(List<AsyncDownloader.SizedFile> files, String baseURL, Path targetDir, DownloadCallback callback, ExecutorService executor, int threads) throws Exception {
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
                DownloadTask task = sendAsync(file, baseUri, targetDir, callback);
                task.completableFuture.thenAccept(consumerObject.next).exceptionally(ec -> {
                    future.completeExceptionally(ec);
                    return null;
                });
            } catch (Exception exception) {
                LogHelper.error(exception);
                future.completeExceptionally(exception);
            }
        };
        consumerObject.next = next;
        for (int i = 0; i < threads; ++i) {
            next.accept(null);
        }
        return future;
    }

    protected DownloadTask sendAsync(AsyncDownloader.SizedFile file, URI baseUri, Path targetDir, DownloadCallback callback) throws Exception {
        IOHelper.createParentDirs(targetDir.resolve(file.filePath));
        ProgressTrackingBodyHandler<Path> bodyHandler = makeBodyHandler(targetDir.resolve(file.filePath), callback);
        CompletableFuture<HttpResponse<Path>> future = client.sendAsync(makeHttpRequest(baseUri, file.urlPath), bodyHandler);
        var ref = new Object() {
            DownloadTask task = null;
        };
        ref.task = new DownloadTask(bodyHandler, future.thenApply((e) -> {
            tasks.remove(ref.task);
            return e;
        }));
        tasks.add(ref.task);
        return ref.task;
    }

    protected HttpRequest makeHttpRequest(URI baseUri, String filePath) throws URISyntaxException {
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

    protected ProgressTrackingBodyHandler<Path> makeBodyHandler(Path file, DownloadCallback callback) {
        return new ProgressTrackingBodyHandler<>(HttpResponse.BodyHandlers.ofFile(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE), callback);
    }

    public interface DownloadCallback {
        void apply(long fullDiff);

        void onComplete(Path path);
    }

    private static class ConsumerObject {
        Consumer<HttpResponse<Path>> next = null;
    }

    public static class DownloadTask {
        public final ProgressTrackingBodyHandler<Path> bodyHandler;
        public final CompletableFuture<HttpResponse<Path>> completableFuture;

        public DownloadTask(ProgressTrackingBodyHandler<Path> bodyHandler, CompletableFuture<HttpResponse<Path>> completableFuture) {
            this.bodyHandler = bodyHandler;
            this.completableFuture = completableFuture;
        }

        public boolean isCompleted() {
            return completableFuture.isDone() | completableFuture.isCompletedExceptionally();
        }

        public void cancel() {
            bodyHandler.cancel();
        }
    }

    public static class ProgressTrackingBodyHandler<T> implements HttpResponse.BodyHandler<T> {
        private final HttpResponse.BodyHandler<T> delegate;
        private final DownloadCallback callback;
        private ProgressTrackingBodySubscriber subscriber;
        private boolean isCanceled = false;

        public ProgressTrackingBodyHandler(HttpResponse.BodyHandler<T> delegate, DownloadCallback callback) {
            this.delegate = delegate;
            this.callback = callback;
        }

        @Override
        public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
            subscriber = new ProgressTrackingBodySubscriber(delegate.apply(responseInfo));
            if (isCanceled) {
                subscriber.cancel();
            }
            return subscriber;
        }

        public void cancel() {
            isCanceled = true;
            if (subscriber != null) {
                subscriber.cancel();
            }
        }

        private class ProgressTrackingBodySubscriber implements HttpResponse.BodySubscriber<T> {
            private final HttpResponse.BodySubscriber<T> delegate;
            private Flow.Subscription subscription;
            private boolean isCanceled = false;

            public ProgressTrackingBodySubscriber(HttpResponse.BodySubscriber<T> delegate) {
                this.delegate = delegate;
            }

            @Override
            public CompletionStage<T> getBody() {
                return delegate.getBody();
            }

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                if (isCanceled) {
                    subscription.cancel();
                }
                delegate.onSubscribe(subscription);
            }

            @Override
            public void onNext(List<ByteBuffer> byteBuffers) {
                long diff = 0;
                for (ByteBuffer buffer : byteBuffers) {
                    diff += buffer.remaining();
                }
                if (callback != null) callback.apply(diff);
                delegate.onNext(byteBuffers);
            }

            @Override
            public void onError(Throwable throwable) {
                delegate.onError(throwable);
            }

            @Override
            public void onComplete() {
                delegate.onComplete();
            }

            public void cancel() {
                isCanceled = true;
                if (subscription != null) {
                    subscription.cancel();
                }
            }
        }
    }
}
