package pro.gravit.launcher.base;

import pro.gravit.launcher.core.CertificatePinningTrustManager;
import pro.gravit.launcher.core.LauncherInject;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Downloader {
    @LauncherInject("launcher.certificatePinning")
    private static boolean isCertificatePinning;
    @LauncherInject("launcher.noHttp2")
    private static boolean isNoHttp2;
    private static volatile SSLSocketFactory sslSocketFactory;
    private static volatile SSLContext sslContext;
    protected final HttpClient client;
    protected final ExecutorService executor;
    protected final Queue<DownloadTask> tasks = new ConcurrentLinkedDeque<>();
    protected CompletableFuture<Void> future;
    protected Downloader(HttpClient client, ExecutorService executor) {
        this.client = client;
        this.executor = executor;
    }

    public static ThreadFactory getDaemonThreadFactory(String name) {
        return (task) -> {
            Thread thread = new Thread(task);
            thread.setName(name);
            thread.setDaemon(true);
            return thread;
        };
    }

    public static HttpClient.Builder newHttpClientBuilder() {
        try {
            if(isCertificatePinning) {
                return HttpClient.newBuilder()
                        .sslContext(makeSSLContext())
                        .version(isNoHttp2 ? HttpClient.Version.HTTP_1_1 : HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.NORMAL);
            } else {
                return HttpClient.newBuilder()
                        .version(isNoHttp2 ? HttpClient.Version.HTTP_1_1 : HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.NORMAL);
            }
        } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException |
                 KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public static SSLSocketFactory makeSSLSocketFactory() throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, KeyManagementException {
        if (sslSocketFactory != null) return sslSocketFactory;
        SSLContext sslContext = makeSSLContext();
        sslSocketFactory = sslContext.getSocketFactory();
        return sslSocketFactory;
    }

    public static SSLContext makeSSLContext() throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, KeyManagementException {
        if (sslContext != null) return sslContext;
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, CertificatePinningTrustManager.getTrustManager().getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    public static Downloader downloadFile(URI uri, Path path, ExecutorService executor) {
        boolean closeExecutor = false;
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor(getDaemonThreadFactory("Downloader"));
            closeExecutor = true;
        }
        Downloader downloader = newDownloader(executor);
        downloader.future = downloader.downloadFile(uri, path);
        if (closeExecutor) {
            ExecutorService finalExecutor = executor;
            downloader.future = downloader.future.thenAccept((e) -> finalExecutor.shutdownNow()).exceptionallyCompose((ex) -> {
                finalExecutor.shutdownNow();
                return CompletableFuture.failedFuture(ex);
            });
        }
        return downloader;
    }

    public static Downloader downloadList(List<SizedFile> files, String baseURL, Path targetDir, DownloadCallback callback, ExecutorService executor, int threads) throws Exception {
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
            downloader.future = downloader.future.thenAccept((e) -> finalExecutor.shutdownNow()).exceptionallyCompose((ex) -> {
                finalExecutor.shutdownNow();
                return CompletableFuture.failedFuture(ex);
            });
        }
        return downloader;
    }

    public static Downloader newDownloader(ExecutorService executor) {
        if (executor == null) {
            throw new NullPointerException();
        }
        HttpClient.Builder builder = newHttpClientBuilder()
                .executor(executor);
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
    }

    public boolean isCanceled() {
        return executor.isTerminated();
    }

    public CompletableFuture<Void> getFuture() {
        return future;
    }

    public CompletableFuture<Void> downloadFile(URI uri, Path path) {
        try {
            IOHelper.createParentDirs(path);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
        return client.sendAsync(HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .build(), HttpResponse.BodyHandlers.ofFile(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)).thenCompose((t) -> {
                    if(t.statusCode() < 200 || t.statusCode() >= 400) {
                        return CompletableFuture.failedFuture(new IOException(String.format("Failed to download %s: code %d", uri.toString(), t.statusCode())));
                    }
                    return CompletableFuture.completedFuture(null);
        });
    }

    public CompletableFuture<Void> downloadFile(String url, Path path, DownloadCallback callback, ExecutorService executor) throws Exception {
        return downloadFiles(new ArrayList<>(List.of(new SizedFile(url, path.getFileName().toString()))), null,
                path.getParent(), callback, executor, 1);
    }

    public CompletableFuture<Void> downloadFile(String url, Path path, long size, DownloadCallback callback, ExecutorService executor) throws Exception {
        return downloadFiles(new ArrayList<>(List.of(new SizedFile(url, path.getFileName().toString(), size))), null,
                path.getParent(), callback, executor, 1);
    }

    public CompletableFuture<Void> downloadFiles(List<SizedFile> files, String baseURL, Path targetDir, DownloadCallback callback, ExecutorService executor, int threads) throws Exception {
        // URI scheme
        URI baseUri = baseURL == null ? null : new URI(baseURL);
        Collections.shuffle(files);
        Queue<SizedFile> queue = new ConcurrentLinkedDeque<>(files);
        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicInteger currentThreads = new AtomicInteger(threads);
        ConsumerObject consumerObject = new ConsumerObject();
        Consumer<HttpResponse<Path>> next = e -> {
            if (callback != null && e != null) {
                callback.onComplete(e.body());
            }
            SizedFile file = queue.poll();
            if (file == null) {
                if (currentThreads.decrementAndGet() == 0)
                    future.complete(null);
                return;
            }
            try {
                DownloadTask task = sendAsync(file, baseUri, targetDir, callback);
                task.completableFuture.thenCompose((res) -> {
                    if(res.statusCode() < 200 || res.statusCode() >= 300) {
                        return CompletableFuture.failedFuture(new IOException(String.format("Failed to download %s: code %d",
                                file.urlPath != null ? file.urlPath /* TODO: baseUri */ : file.filePath, res.statusCode())));
                    }
                    return CompletableFuture.completedFuture(res);
                }).thenAccept(consumerObject.next).exceptionally(ec -> {
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

    protected DownloadTask sendAsync(SizedFile file, URI baseUri, Path targetDir, DownloadCallback callback) throws Exception {
        IOHelper.createParentDirs(targetDir.resolve(file.filePath));
        ProgressTrackingBodyHandler<Path> bodyHandler = makeBodyHandler(targetDir.resolve(file.filePath), callback);
        CompletableFuture<HttpResponse<Path>> future = client.sendAsync(makeHttpRequest(baseUri, file.urlPath), bodyHandler);
        AtomicReference<DownloadTask> task = new AtomicReference<>(null);
        task.set(new DownloadTask(bodyHandler, null /* fix NPE (future already completed) */));
        tasks.add(task.get());
        task.get().completableFuture = future.thenApply((e) -> {
                    tasks.remove(task.get());
                    return e;
                });
        return task.get();
    }

    protected HttpRequest makeHttpRequest(URI baseUri, String filePath) throws URISyntaxException {
        URI uri;
        if(baseUri != null) {
            String scheme = baseUri.getScheme();
            String host = baseUri.getHost();
            int port = baseUri.getPort();
            if (port != -1)
                host = host + ":" + port;
            String path = baseUri.getPath();
            uri = new URI(scheme, host, path + filePath, "", "");
        } else {
            uri = new URI(filePath);
        }
        return HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36")
                .build();
    }

    protected ProgressTrackingBodyHandler<Path> makeBodyHandler(Path file, DownloadCallback callback) {
        return new ProgressTrackingBodyHandler<>(HttpResponse.BodyHandlers.ofFile(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING), callback);
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
        public CompletableFuture<HttpResponse<Path>> completableFuture;

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

    public static class SizedFile {
        public final String urlPath, filePath;
        public final long size;

        public SizedFile(String path, long size) {
            this.urlPath = path;
            this.filePath = path;
            this.size = size;
        }

        public SizedFile(String urlPath, String filePath, long size) {
            this.urlPath = urlPath;
            this.filePath = filePath;
            this.size = size;
        }

        public SizedFile(String urlPath, String filePath) {
            this.urlPath = urlPath;
            this.filePath = filePath;
            this.size = -1;
        }
    }
}
