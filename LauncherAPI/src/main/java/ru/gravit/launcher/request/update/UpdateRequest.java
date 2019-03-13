package ru.gravit.launcher.request.update;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.events.request.UpdateRequestEvent;
import ru.gravit.launcher.hasher.FileNameMatcher;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.hasher.HashedEntry;
import ru.gravit.launcher.hasher.HashedFile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.request.UpdateAction;
import ru.gravit.launcher.request.update.UpdateRequest.State.Callback;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.SecurityHelper.DigestAlgorithm;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SignatureException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.zip.InflaterInputStream;

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

    private static void fillActionsQueue(Queue<UpdateAction> queue, HashedDir mismatch) {
        for (Entry<String, HashedEntry> mapEntry : mismatch.map().entrySet()) {
            String name = mapEntry.getKey();
            HashedEntry entry = mapEntry.getValue();
            HashedEntry.Type entryType = entry.getType();
            switch (entryType) {
                case DIR: // cd - get - cd ..
                    queue.add(new UpdateAction(UpdateAction.Type.CD, name, entry));
                    fillActionsQueue(queue, (HashedDir) entry);
                    queue.add(UpdateAction.CD_BACK);
                    break;
                case FILE: // get
                    queue.add(new UpdateAction(UpdateAction.Type.GET, name, entry));
                    break;
                default:
                    throw new AssertionError("Unsupported hashed entry type: " + entryType.name());
            }
        }
    }
    @Override
    public UpdateRequestEvent requestWebSockets() throws Exception
    {
        return (UpdateRequestEvent) LegacyRequestBridge.sendRequest(this);
    }

    // Instance
    private final String dirName;
    private final Path dir;
    private final FileNameMatcher matcher;

    private final boolean digest;
    private volatile Callback stateCallback;
    // State
    private HashedDir localDir;
    private long totalDownloaded;

    private long totalSize;

    private Instant startTime;

    @LauncherAPI
    public UpdateRequest(LauncherConfig config, String dirName, Path dir, FileNameMatcher matcher, boolean digest) {
        super(config);
        this.dirName = IOHelper.verifyFileName(dirName);
        this.dir = Objects.requireNonNull(dir, "dir");
        this.matcher = matcher;
        this.digest = digest;
    }

    @LauncherAPI
    public UpdateRequest(String dirName, Path dir, FileNameMatcher matcher, boolean digest) {
        this(null, dirName, dir, matcher, digest);
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

    private void downloadFile(Path file, HashedFile hFile, InputStream input) throws IOException {
        String filePath = IOHelper.toString(dir.relativize(file));
        updateState(filePath, 0L, hFile.size);

        // Start file update
        MessageDigest digest = this.digest ? SecurityHelper.newDigest(DigestAlgorithm.MD5) : null;
        try (OutputStream fileOutput = IOHelper.newOutput(file)) {
            long downloaded = 0L;

            // Download with digest update
            byte[] bytes = IOHelper.newBuffer();
            while (downloaded < hFile.size) {
                int remaining = (int) Math.min(hFile.size - downloaded, bytes.length);
                int length = input.read(bytes, 0, remaining);
                if (length < 0)
                    throw new EOFException(String.format("%d bytes remaining", hFile.size - downloaded));

                // Update file
                fileOutput.write(bytes, 0, length);
                if (digest != null)
                    digest.update(bytes, 0, length);

                // Update state
                downloaded += length;
                totalDownloaded += length;
                updateState(filePath, downloaded, hFile.size);
            }
        }

        // Verify digest
        if (digest != null) {
            byte[] digestBytes = digest.digest();
            if (!hFile.isSameDigest(digestBytes))
                throw new SecurityException(String.format("File digest mismatch: '%s'", filePath));
        }
    }

    @Override
    public Integer getLegacyType() {
        return RequestType.UPDATE.getNumber();
    }

    @Override
    public UpdateRequestEvent request() throws Exception {
        Files.createDirectories(dir);
        localDir = new HashedDir(dir, matcher, false, digest);

        // Start request
        return super.request();
    }

    @Override
    protected UpdateRequestEvent requestDo(HInput input, HOutput output) throws IOException, SignatureException {
        // Write update dir name
        output.writeString(dirName, 255);
        output.flush();
        readError(input);

        // Get diff between local and remote dir
        SignedObjectHolder<HashedDir> remoteHDirHolder = new SignedObjectHolder<>(input, config.publicKey, HashedDir::new);
        HashedDir hackHackedDir = remoteHDirHolder.object;
        Launcher.profile.pushOptionalFile(hackHackedDir, !Launcher.profile.isUpdateFastCheck());
        HashedDir.Diff diff = hackHackedDir.diff(localDir, matcher);
        totalSize = diff.mismatch.size();
        boolean compress = input.readBoolean();

        // Build actions queue
        Queue<UpdateAction> queue = new LinkedList<>();
        fillActionsQueue(queue, diff.mismatch);
        queue.add(UpdateAction.FINISH);

        // noinspection IOResourceOpenedButNotSafelyClosed
        InputStream fileInput = compress ? new InflaterInputStream(input.stream, IOHelper.newInflater(), IOHelper.BUFFER_SIZE) : input.stream;

        // Download missing first
        // (otherwise it will cause mustdie indexing bug)
        startTime = Instant.now();
        Path currentDir = dir;
        UpdateAction[] actionsSlice = new UpdateAction[SerializeLimits.MAX_QUEUE_SIZE];
        while (!queue.isEmpty()) {
            int length = Math.min(queue.size(), SerializeLimits.MAX_QUEUE_SIZE);

            // Write actions slice
            output.writeLength(length, SerializeLimits.MAX_QUEUE_SIZE);
            for (int i = 0; i < length; i++) {
                UpdateAction action = queue.remove();
                actionsSlice[i] = action;
                action.write(output);
            }
            output.flush();

            // Perform actions
            for (int i = 0; i < length; i++) {
                UpdateAction action = actionsSlice[i];
                switch (action.type) {
                    case CD:
                        currentDir = currentDir.resolve(action.name);
                        Files.createDirectories(currentDir);
                        break;
                    case GET:
                        Path targetFile = currentDir.resolve(action.name);
                        if (fileInput.read() != 0xFF)
                            throw new IOException("Serverside cached size mismath for file " + action.name);
                        downloadFile(targetFile, (HashedFile) action.entry, fileInput);
                        break;
                    case CD_BACK:
                        currentDir = currentDir.getParent();
                        break;
                    case FINISH:
                        break;
                    default:
                        throw new AssertionError(String.format("Unsupported action type: '%s'", action.type.name()));
                }
            }
        }

        // Write update completed packet
        deleteExtraDir(dir, diff.extra, diff.extra.flag);
        return new UpdateRequestEvent(remoteHDirHolder.object);
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
