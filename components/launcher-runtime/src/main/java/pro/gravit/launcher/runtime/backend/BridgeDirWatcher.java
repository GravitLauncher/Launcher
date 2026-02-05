package pro.gravit.launcher.runtime.backend;

import pro.gravit.launcher.client.utils.DirWatcher;
import pro.gravit.launcher.core.hasher.FileNameMatcher;
import pro.gravit.launcher.core.hasher.HashedDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

public class BridgeDirWatcher extends DirWatcher {
    private final ReadyProfileImpl readyProfile;

    public BridgeDirWatcher(Path dir, HashedDir hdir, FileNameMatcher matcher, boolean digest, ReadyProfileImpl readyProfile) throws IOException {
        super(dir, hdir, matcher, digest);
        this.readyProfile = readyProfile;
    }

    @Override
    protected void onForbiddenModification(WatchEvent<?> event, Path path) {
        String error = String.format("[Watcher] Forbidden modification (%s, %d times): '%s'", event.kind(), event.count(), path);
        byte[] outputBytes = error.getBytes(StandardCharsets.UTF_8);
        readyProfile.callback.onErrorOutput(outputBytes, 0, outputBytes.length);
        readyProfile.terminate();
    }
}
