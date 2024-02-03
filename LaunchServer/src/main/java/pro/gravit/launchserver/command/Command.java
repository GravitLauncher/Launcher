package pro.gravit.launchserver.command;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.Downloader;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.command.CommandException;

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

    protected ClientProfile.Version parseClientVersion(String arg) throws CommandException {
        if(arg.isEmpty()) {
            throw new CommandException("ClientVersion can't be empty");
        }
        return Launcher.gsonManager.gson.fromJson(arg, ClientProfile.Version.class);
    }

    protected boolean showApplyDialog(String text) throws IOException {
        System.out.printf("%s [Y/N]:", text);
        String response = server.commandHandler.readLine().toLowerCase(Locale.ROOT);
        return response.equals("y");
    }

    protected Downloader downloadWithProgressBar(String taskName, List<Downloader.SizedFile> list, String baseUrl, Path targetDir) throws Exception {
        long total = 0;
        for (Downloader.SizedFile file : list) {
            if(file.size < 0) {
                continue;
            }
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
        bar.setExtraMessage(" [0/%d]".formatted(totalFiles));
        Downloader downloader = Downloader.downloadList(list, baseUrl, targetDir, new Downloader.DownloadCallback() {
            @Override
            public void apply(long fullDiff) {
                current.addAndGet(fullDiff);
                bar.stepBy(fullDiff);
            }

            @Override
            public void onComplete(Path path) {
                bar.setExtraMessage(" [%d/%d]".formatted(currentFiles.incrementAndGet(), totalFiles));
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
