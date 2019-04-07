package ru.gravit.launcher.request.update;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.downloader.ListDownloader;
import ru.gravit.launcher.events.request.UpdateRequestEvent;
import ru.gravit.launcher.hasher.FileNameMatcher;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.hasher.HashedEntry;
import ru.gravit.launcher.hasher.HashedFile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.update.UpdateRequest.State.Callback;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

public final class UpdateRequest extends Request<UpdateRequestEvent> implements RequestInterface {

    @Override
    public String getType() {
        return "update";
    }

    public static final class State {
        @FunctionalInterface
        public interface Callback {
            @LauncherAPI
            void call(State state);
        }

        @LauncherAPI
        public final long fileDownloaded;
        @LauncherAPI
        public final long fileSize;
        @LauncherAPI
        public final long totalDownloaded;
        @LauncherAPI
        public final long totalSize;
        @LauncherAPI
        public final String filePath;

        @LauncherAPI
        public final Duration duration;

        public State(String filePath, long fileDownloaded, long fileSize, long totalDownloaded, long totalSize, Duration duration) {
            this.filePath = filePath;
            this.fileDownloaded = fileDownloaded;
            this.fileSize = fileSize;
            this.totalDownloaded = totalDownloaded;
            this.totalSize = totalSize;

            // Also store time of creation
            this.duration = duration;
        }

        @LauncherAPI
        public double getBps() {
            long seconds = duration.getSeconds();
            if (seconds == 0)
                return -1.0D; // Otherwise will throw /0 exception
            return totalDownloaded / (double) seconds;
        }

        @LauncherAPI
        public Duration getEstimatedTime() {
            double bps = getBps();
            if (bps <= 0.0D)
                return null; // Otherwise will throw /0 exception
            return Duration.ofSeconds((long) (getTotalRemaining() / bps));
        }

        @LauncherAPI
        public double getFileDownloadedKiB() {
            return fileDownloaded / 1024.0D;
        }

        @LauncherAPI
        public double getFileDownloadedMiB() {
            return getFileDownloadedKiB() / 1024.0D;
        }

        @LauncherAPI
        public double getFileDownloadedPart() {
            if (fileSize == 0)
                return 0.0D;
            return (double) fileDownloaded / fileSize;
        }

        @LauncherAPI
        public long getFileRemaining() {
            return fileSize - fileDownloaded;
        }

        @LauncherAPI
        public double getFileRemainingKiB() {
            return getFileRemaining() / 1024.0D;
        }

        @LauncherAPI
        public double getFileRemainingMiB() {
            return getFileRemainingKiB() / 1024.0D;
        }

        @LauncherAPI
        public double getFileSizeKiB() {
            return fileSize / 1024.0D;
        }

        @LauncherAPI
        public double getFileSizeMiB() {
            return getFileSizeKiB() / 1024.0D;
        }

        @LauncherAPI
        public double getTotalDownloadedKiB() {
            return totalDownloaded / 1024.0D;
        }

        @LauncherAPI
        public double getTotalDownloadedMiB() {
            return getTotalDownloadedKiB() / 1024.0D;
        }

        @LauncherAPI
        public double getTotalDownloadedPart() {
            if (totalSize == 0)
                return 0.0D;
            return (double) totalDownloaded / totalSize;
        }

        @LauncherAPI
        public long getTotalRemaining() {
            return totalSize - totalDownloaded;
        }

        @LauncherAPI
        public double getTotalRemainingKiB() {
            return getTotalRemaining() / 1024.0D;
        }

        @LauncherAPI
        public double getTotalRemainingMiB() {
            return getTotalRemainingKiB() / 1024.0D;
        }

        @LauncherAPI
        public double getTotalSizeKiB() {
            return totalSize / 1024.0D;
        }

        @LauncherAPI
        public double getTotalSizeMiB() {
            return getTotalSizeKiB() / 1024.0D;
        }
    }

    @Override
    public UpdateRequestEvent requestDo() throws Exception {
        LogHelper.debug("Start update request");
        UpdateRequestEvent e = (UpdateRequestEvent) LegacyRequestBridge.sendRequest(this);
        LogHelper.debug("Start update");
        Launcher.profile.pushOptionalFile(e.hdir, !Launcher.profile.isUpdateFastCheck());
        HashedDir.Diff diff = e.hdir.diff(localDir, matcher);
        final List<ListDownloader.DownloadTask> adds = new ArrayList<>();
        diff.mismatch.walk(IOHelper.CROSS_SEPARATOR, (path, name, entry) -> {
            if(entry.getType() == HashedEntry.Type.FILE) {
                HashedFile file = (HashedFile) entry;
                totalSize += file.size;
                adds.add(new ListDownloader.DownloadTask(path, file.size));
            }
        });
        totalSize = diff.mismatch.size();
        startTime = Instant.now();
        updateState("UnknownFile", 0L, 100);
        ListDownloader listDownloader = new ListDownloader();
        listDownloader.download(e.url, adds, dir, this::updateState, (add) -> {
            totalDownloaded += add;
        });
        deleteExtraDir(dir, diff.extra, diff.extra.flag);
        LogHelper.debug("Update success");
        return e;
    }

    // Instance
    @LauncherNetworkAPI
    private final String dirName;
    private transient final Path dir;
    private transient final FileNameMatcher matcher;

    private transient final boolean digest;
    private transient volatile Callback stateCallback;
    // State
    private transient HashedDir localDir;
    private transient long totalDownloaded;

    private transient long totalSize;

    private transient Instant startTime;

    @LauncherAPI
    public UpdateRequest(String dirName, Path dir, FileNameMatcher matcher, boolean digest) {
        this.dirName = IOHelper.verifyFileName(dirName);
        this.dir = Objects.requireNonNull(dir, "dir");
        this.matcher = matcher;
        this.digest = digest;
    }

    private void deleteExtraDir(Path subDir, HashedDir subHDir, boolean flag) throws IOException {
        for (Entry<String, HashedEntry> mapEntry : subHDir.map().entrySet()) {
            String name = mapEntry.getKey();
            Path path = subDir.resolve(name);

            // Delete list and dirs based on type
            HashedEntry entry = mapEntry.getValue();
            HashedEntry.Type entryType = entry.getType();
            switch (entryType) {
                case FILE:
                    updateState(IOHelper.toString(path), 0, 0);
                    Files.delete(path);
                    break;
                case DIR:
                    deleteExtraDir(path, (HashedDir) entry, flag || entry.flag);
                    break;
                default:
                    throw new AssertionError("Unsupported hashed entry type: " + entryType.name());
            }
        }

        // Delete!
        if (flag) {
            updateState(IOHelper.toString(subDir), 0, 0);
            Files.delete(subDir);
        }
    }

    @Override
    public UpdateRequestEvent request() throws Exception {
        Files.createDirectories(dir);
        localDir = new HashedDir(dir, matcher, false, digest);

        // Start request
        return super.request();
    }

    @LauncherAPI
    public void setStateCallback(Callback callback) {
        stateCallback = callback;
    }

    private void updateState(String filePath, long fileDownloaded, long fileSize) {
        if (stateCallback != null)
            stateCallback.call(new State(filePath, fileDownloaded, fileSize,
                    totalDownloaded, totalSize, Duration.between(startTime, Instant.now())));
    }
}
