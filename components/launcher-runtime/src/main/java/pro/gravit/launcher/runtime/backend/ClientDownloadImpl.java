package pro.gravit.launcher.runtime.backend;

import pro.gravit.launcher.base.Downloader;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.optional.OptionalView;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalActionFile;
import pro.gravit.launcher.core.api.LauncherAPIHolder;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.core.backend.LauncherBackendAPI;
import pro.gravit.launcher.core.hasher.FileNameMatcher;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launcher.core.hasher.HashedEntry;
import pro.gravit.launcher.core.hasher.HashedFile;
import pro.gravit.launcher.runtime.client.DirBridge;
import pro.gravit.launcher.runtime.utils.AssetIndexHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class ClientDownloadImpl {
    private LauncherBackendImpl backend;

    ClientDownloadImpl(LauncherBackendImpl backend) {
        this.backend = backend;
    }

    CompletableFuture<LauncherBackendAPI.ReadyProfile> downloadProfile(ClientProfile profile, ProfileSettingsImpl settings, LauncherBackendAPI.DownloadCallback callback) {
        AtomicReference<DownloadedDir> clientRef = new AtomicReference<>();
        AtomicReference<DownloadedDir> assetRef = new AtomicReference<>();
        AtomicReference<DownloadedDir> javaRef = new AtomicReference<>();
        return LauncherAPIHolder.profile().changeCurrentProfile(profile)
                .thenCompose(vv -> downloadDir(profile.getDir(), profile.getClientUpdateMatcher(), settings.view, callback)).thenCompose((clientDir -> {
            clientRef.set(clientDir);
            return downloadAsset(profile.getAssetDir(), profile.getAssetUpdateMatcher(), profile.getAssetIndex(), callback);
        })).thenCompose(assetDir -> {
            assetRef.set(assetDir);
            return CompletableFuture.completedFuture((DownloadedDir)null); // TODO Custom Java
        }).thenCompose(javaDir -> {
            javaRef.set(javaDir);
            return CompletableFuture.completedFuture(null);
        }).thenApply(v -> {
            return new ReadyProfileImpl(backend, profile, settings, clientRef.get(), assetRef.get(), javaRef.get());
        });
    }

    CompletableFuture<DownloadedDir> downloadAsset(String dirName, FileNameMatcher matcher, String assetIndexString, LauncherBackendAPI.DownloadCallback callback) {
        Path targetDir = DirBridge.dirUpdates.resolve(dirName);
        Path assetIndexPath = targetDir.resolve("indexes").resolve(assetIndexString+".json");
        return LauncherAPIHolder.profile().fetchUpdateInfo(dirName).thenComposeAsync((response) -> {
                    callback.onStage(LauncherBackendAPI.DownloadCallback.STAGE_ASSET_VERIFY);
                    return verifyAssetIndex(assetIndexString, response, assetIndexPath, targetDir);
                }, backend.executorService)
                    .thenApply(assetData -> {
                        HashedDir dir = assetData.updateInfo.getHashedDir();
                        AssetIndexHelper.modifyHashedDir(assetData.index, dir);
                        return new VirtualUpdateInfo(dir, assetData.updateInfo.getUrl());
                    })
                    .thenCompose(response -> downloadDir(targetDir, response, matcher, callback, e -> e));
    }

    private CompletableFuture<AssetData> verifyAssetIndex(String assetIndexString, ProfileFeatureAPI.UpdateInfo response, Path assetIndexPath, Path targetDir) {
        var assetIndexRelPath = String.format("indexes/%s.json", assetIndexString);
        var assetIndexHash = response.getHashedDir().findRecursive(assetIndexRelPath);
        if(!(assetIndexHash.entry instanceof HashedFile assetIndexHashFile)) {
            return CompletableFuture.failedFuture(new FileNotFoundException(String.format("Asset Index %s not found in the server response", assetIndexString)));
        }
        try {
            if(Files.exists(assetIndexPath) && assetIndexHashFile.isSame(assetIndexPath, true)) {
                var assetIndex = AssetIndexHelper.parse(assetIndexPath);
                return CompletableFuture.completedFuture(new AssetData(response, assetIndex));
            } else {
                var downloader = Downloader.newDownloader(backend.executorService);
                var list = new LinkedList<Downloader.SizedFile>();
                list.add(new Downloader.SizedFile(assetIndexRelPath, assetIndexRelPath, assetIndexHashFile.size));
                return downloader.downloadFiles(list, response.getUrl(), targetDir, null, backend.executorService, 1).thenComposeAsync(v -> {
                    try {
                        var assetIndex = AssetIndexHelper.parse(assetIndexPath);
                        return CompletableFuture.completedFuture(new AssetData(response, assetIndex));
                    } catch (IOException e) {
                        return CompletableFuture.failedFuture(e);
                    }
                }, backend.executorService);
            }
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    CompletableFuture<DownloadedDir> downloadDir(String dirName, FileNameMatcher matcher, LauncherBackendAPI.DownloadCallback callback) {
        Path targetDir = DirBridge.dirUpdates.resolve(dirName);
        return LauncherAPIHolder.profile().fetchUpdateInfo(dirName)
                .thenCompose(response -> downloadDir(targetDir, response, matcher, callback, e -> e));
    }

    CompletableFuture<DownloadedDir> downloadDir(String dirName, FileNameMatcher matcher, OptionalView view, LauncherBackendAPI.DownloadCallback callback) {
        Path targetDir = DirBridge.dirUpdates.resolve(dirName);
        return LauncherAPIHolder.profile().fetchUpdateInfo(dirName)
                .thenCompose(response -> {
                    var hashedDir = response.getHashedDir();
                    var remap = applyOptionalMods(view, hashedDir);
                    return downloadDir(targetDir, new VirtualUpdateInfo(hashedDir, response.getUrl()), matcher, callback, makePathRemapperFunction(remap));
                });
    }

    CompletableFuture<DownloadedDir> downloadDir(Path targetDir, ProfileFeatureAPI.UpdateInfo updateInfo, FileNameMatcher matcher, LauncherBackendAPI.DownloadCallback callback, Function<String, String> remap) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                callback.onStage(LauncherBackendAPI.DownloadCallback.STAGE_HASHING);
                if(!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                HashedDir realFiles = new HashedDir(targetDir, matcher, false, true);
                callback.onStage(LauncherBackendAPI.DownloadCallback.STAGE_DIFF);
                return updateInfo.getHashedDir().diff(realFiles, matcher);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, backend.executorService).thenComposeAsync((diff) -> {
            return downloadFiles(targetDir, updateInfo, callback, diff, remap);
        }, backend.executorService).thenApply(v -> new DownloadedDir(updateInfo.getHashedDir(), targetDir));
    }

    private CompletableFuture<HashedDir.Diff> downloadFiles(Path targetDir, ProfileFeatureAPI.UpdateInfo updateInfo, LauncherBackendAPI.DownloadCallback callback, HashedDir.Diff diff, Function<String, String> remap) {
        Downloader downloader = Downloader.newDownloader(backend.executorService);
        try {
            var files = collectFilesAndCreateDirectories(targetDir, diff.mismatch, remap);
            long total = 0;
            for(var e : files) {
                total += e.size;
            }
            callback.onTotalDownload(total);
            callback.onCanCancel(downloader::cancel);
            return downloader.downloadFiles(files, updateInfo.getUrl(), targetDir, new Downloader.DownloadCallback() {
                @Override
                public void apply(long fullDiff) {
                    callback.onCurrentDownloaded(fullDiff);
                }

                @Override
                public void onComplete(Path path) {

                }
            }, backend.executorService, 4).thenComposeAsync(v -> {
                callback.onCanCancel(null);
                callback.onStage(LauncherBackendAPI.DownloadCallback.STAGE_DELETE_EXTRA);
                try {
                    deleteExtraDir(targetDir, diff.extra, diff.extra.flag);
                } catch (IOException ex) {
                    return CompletableFuture.failedFuture(ex);
                }
                callback.onStage(LauncherBackendAPI.DownloadCallback.STAGE_DONE_PART);
                return CompletableFuture.completedFuture(diff);
            }, backend.executorService);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private List<Downloader.SizedFile> collectFilesAndCreateDirectories(Path dir, HashedDir mismatch, Function<String, String> pathRemapper) throws IOException {
        List<Downloader.SizedFile> files = new ArrayList<>();
        mismatch.walk(File.separator, (path, name, entry) -> {
            if(entry.getType() == HashedEntry.Type.DIR) {
                var dirPath = dir.resolve(path);
                try {
                    if(!Files.exists(dirPath)) {
                        Files.createDirectory(dirPath);
                    } else if (!Files.isDirectory(dirPath)) {
                        throw new IOException(String.format("%s is not a directory", path));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return HashedDir.WalkAction.CONTINUE;
            }
            String pathFixed = path.replace(File.separatorChar, '/');
            files.add(new Downloader.SizedFile(pathFixed, pathRemapper.apply(pathFixed), entry.size()));
            return HashedDir.WalkAction.CONTINUE;
        });
        return files;
    }

    private void deleteExtraDir(Path subDir, HashedDir subHDir, boolean deleteDir) throws IOException {
        for (Map.Entry<String, HashedEntry> mapEntry : subHDir.map().entrySet()) {
            String name = mapEntry.getKey();
            Path path = subDir.resolve(name);

            // Delete list and dirs based on type
            HashedEntry entry = mapEntry.getValue();
            HashedEntry.Type entryType = entry.getType();
            switch (entryType) {
                case FILE -> Files.delete(path);
                case DIR -> deleteExtraDir(path, (HashedDir) entry, deleteDir || entry.flag);
                default -> throw new AssertionError("Unsupported hashed entry type: " + entryType.name());
            }
        }

        // Delete!
        if (deleteDir) {
            Files.delete(subDir);
        }
    }

    private Function<String, String> makePathRemapperFunction(LinkedList<PathRemapperData> map) {
        return (path) -> {
            for(var e : map) {
                if(path.startsWith(e.key)) {
                    return e.value;
                }
            }
            return path;
        };
    }

    private LinkedList<PathRemapperData> applyOptionalMods(OptionalView view, HashedDir hdir) {
        for (OptionalAction action : view.getDisabledActions()) {
            if (action instanceof OptionalActionFile optionalActionFile) {
                optionalActionFile.disableInHashedDir(hdir);
            }
        }
        LinkedList<PathRemapperData> pathRemapper = new LinkedList<>();
        Set<OptionalActionFile> fileActions = view.getActionsByClass(OptionalActionFile.class);
        for (OptionalActionFile file : fileActions) {
            file.injectToHashedDir(hdir);
            file.files.forEach((k, v) -> {
                if (v == null || v.isEmpty()) return;
                pathRemapper.add(new PathRemapperData(v, k)); //reverse (!)
                LogHelper.dev("Remap prepare %s to %s", v, k);
            });
        }
        pathRemapper.sort(Comparator.comparingInt(c -> -c.key.length())); // Support deep remap
        return pathRemapper;
    }

    private record PathRemapperData(String key, String value) {
    }

    record AssetData(ProfileFeatureAPI.UpdateInfo updateInfo, AssetIndexHelper.AssetIndex index) {

    }

    record DownloadedDir(HashedDir dir, Path path) {

    }

    record VirtualUpdateInfo(HashedDir dir, String url) implements ProfileFeatureAPI.UpdateInfo {

        @Override
        public HashedDir getHashedDir() {
            return dir;
        }

        @Override
        public String getUrl() {
            return url;
        }
    }
}
