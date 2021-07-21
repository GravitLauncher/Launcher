package pro.gravit.utils;

import pro.gravit.launcher.AsyncDownloader;
import pro.gravit.utils.helper.LogHelper;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Downloader {
    private final CompletableFuture<Void> future;
    private final AsyncDownloader asyncDownloader;
    private Downloader(CompletableFuture<Void> future, AsyncDownloader downloader) {
        this.future = future;
        this.asyncDownloader = downloader;
    }

    public static Downloader downloadList(List<AsyncDownloader.SizedFile> files, String baseURL, Path targetDir, DownloadCallback callback, ExecutorService executor, int threads) throws Exception {
        final boolean closeExecutor;
        LogHelper.info("Download with legacy mode");
        if (executor == null) {
            executor = Executors.newWorkStealingPool(4);
            closeExecutor = true;
        } else {
            closeExecutor = false;
        }
        AsyncDownloader asyncDownloader = new AsyncDownloader((diff) -> {
            if (callback != null) {
                callback.apply(diff);
            }
        });
        List<List<AsyncDownloader.SizedFile>> list = asyncDownloader.sortFiles(files, threads);
        CompletableFuture<Void> future = CompletableFuture.allOf(asyncDownloader.runDownloadList(list, baseURL, targetDir, executor));

        ExecutorService finalExecutor = executor;
        return new Downloader(future.thenAccept(e -> {
            if (closeExecutor) {
                finalExecutor.shutdownNow();
            }
        }), asyncDownloader);
    }

    public CompletableFuture<Void> getFuture() {
        return future;
    }

    public void cancel() {
        this.asyncDownloader.isClosed = true;
    }

    public boolean isCanceled() {
        return this.asyncDownloader.isClosed;
    }

    public interface DownloadCallback {
        void apply(long fullDiff);

        void onComplete(Path path);
    }
}
