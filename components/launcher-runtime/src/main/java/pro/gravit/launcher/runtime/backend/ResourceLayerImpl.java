package pro.gravit.launcher.runtime.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.vfs.Vfs;
import pro.gravit.launcher.base.vfs.VfsDirectory;
import pro.gravit.launcher.base.vfs.directory.OverlayVfsDirectory;
import pro.gravit.launcher.core.backend.LauncherBackendAPI;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ResourceLayerImpl implements LauncherBackendAPI.ResourceLayer {

    private static final Logger logger =
            LoggerFactory.getLogger(ResourceLayerImpl.class);

    private final Path vfsPath;

    public ResourceLayerImpl(Path basePath, List<Path> overlayList) {
        if(overlayList == null || overlayList.isEmpty()) {
            vfsPath = basePath;
            return;
        }
        List<VfsDirectory> overlays = new ArrayList<>();
        for(var e : overlayList) {
            var dir = (VfsDirectory) Vfs.get().resolve(basePath.resolve(e));
            if(dir != null) {
                overlays.add(dir);
            }
        }
        overlays.add((VfsDirectory) Vfs.get().resolve(basePath));
        OverlayVfsDirectory directory = new OverlayVfsDirectory(overlays);
        String randomName = SecurityHelper.randomStringToken();
        vfsPath = Path.of(randomName);
        if(logger.isTraceEnabled()) {
            logger.trace("Make overlay {} from {}", vfsPath, basePath);
            for(var e : overlays) {
                logger.trace("Layer {}", e.getClass().getSimpleName());
            }
        }
        Vfs.get().put(vfsPath, directory);
    }

    @Override
    public URL getURL(Path path) throws IOException {
        return Vfs.get().getURL(vfsPath.resolve(path));
    }
}