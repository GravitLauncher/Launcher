package pro.gravit.launchserver.command;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import pro.gravit.launcher.AsyncDownloader;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.Downloader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public abstract class Command extends pro.gravit.utils.command.Command {
    protected final LaunchServer server;


    protected Command(LaunchServer server) {
        super();
        this.server = server;
    }

    public Command(Map<String, pro.gravit.utils.command.Command> childCommands, LaunchServer server) {
        super(childCommands);
        this.server = server;
    }

    protected boolean showApplyDialog(String text) throws IOException {
        System.out.printf("%s [Y/N]:", text);
        String response = server.commandHandler.readLine().toLowerCase(Locale.ROOT);
        return response.equals("y");
    }

    protected Downloader downloadWithProgressBar(String taskName, List<AsyncDownloader.SizedFile> list, String baseUrl, Path targetDir) throws Exception {
        long total = 0;
        for (AsyncDownloader.SizedFile file : list) {
            total += file.size;
        }
        long totalFiles = list.size();
        AtomicLong current = new AtomicLong(0);
        AtomicLong currentFiles = new AtomicLong(0);
        ProgressBar bar = (new ProgressBarBuilder()).setTaskName(taskName)
                .setInitialMax(total)
                .showSpeed()
                .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                .setUnit("MB", 1024 * 1024)
                .build();
        bar.setExtraMessage(String.format(" [0/%d]", totalFiles));
        Downloader downloader = Downloader.downloadList(list, baseUrl, targetDir, new Downloader.DownloadCallback() {
            @Override
            public void apply(long fullDiff) {
                current.addAndGet(fullDiff);
                bar.stepBy(fullDiff);
            }

            @Override
            public void onComplete(Path path) {
                bar.setExtraMessage(String.format(" [%d/%d]", currentFiles.incrementAndGet(), totalFiles));
            }
        }, null, 4);
        downloader.getFuture().handle((v, e) -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            bar.close();
            if (e != null) {
                future.completeExceptionally(e);
            } else {
                future.complete(null);
            }
            return future;
        });
        return downloader;
    }
}
