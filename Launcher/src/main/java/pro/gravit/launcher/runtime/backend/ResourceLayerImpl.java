package pro.gravit.launcher.runtime.backend;

import pro.gravit.launcher.base.vfs.Vfs;
import pro.gravit.launcher.base.vfs.VfsDirectory;
import pro.gravit.launcher.base.vfs.directory.OverlayVfsDirectory;
import pro.gravit.launcher.core.backend.LauncherBackendAPI;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class ResourceLayerImpl implements LauncherBackendAPI.ResourceLayer {
    private final Path vfsPath;

    public ResourceLayerImpl(Path basePath, List<Path> overlayList) {
        if(overlayList == null || overlayList.isEmpty()) {
            vfsPath = basePath;
            return;
        }
        List<VfsDirectory> overlays = new ArrayList<>();
        overlays.add((VfsDirectory) Vfs.get().resolve(basePath));
        for(var e : overlayList) {
            var dir = (VfsDirectory) Vfs.get().resolve(basePath.resolve(e));
            if(dir != null) {
                overlays.add(dir);
            }
        }
        OverlayVfsDirectory directory = new OverlayVfsDirectory(overlays);
        String randomName = SecurityHelper.randomStringToken();
        vfsPath = Path.of(randomName);
        if(LogHelper.isDevEnabled()) {
            LogHelper.dev("Make overlay %s from %s", vfsPath, basePath);
            for(var e : overlays) {
                LogHelper.dev("Layer %s", e.getClass().getSimpleName());
            }
        }
        Vfs.get().put(vfsPath, directory);
    }

    @Override
    public URL getURL(Path path) throws IOException {
        return Vfs.get().getURL(vfsPath.resolve(path));
    }
}
