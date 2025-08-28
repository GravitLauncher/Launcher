package pro.gravit.launcher.runtime.utils;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launcher.core.hasher.HashedEntry;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.*;

public class AssetIndexHelper {

    public static AssetIndex parse(Path path) throws IOException {
        try (Reader reader = IOHelper.newReader(path)) {
            return Launcher.gsonManager.gson.fromJson(reader, AssetIndex.class);
        }
    }

    public static void modifyHashedDir(AssetIndex index, HashedDir original) {
        Set<String> hashes = new HashSet<>();
        for (AssetIndexObject obj : index.objects.values()) {
            hashes.add(obj.hash);
        }
        HashedDir objects = (HashedDir) original.getEntry("objects");
        List<String> toDeleteDirs = new ArrayList<>(16);
        for (Map.Entry<String, HashedEntry> entry : objects.map().entrySet()) {
            if (entry.getValue().getType() != HashedEntry.Type.DIR) {
                continue;
            }
            HashedDir dir = (HashedDir) entry.getValue();
            List<String> toDelete = new ArrayList<>(16);
            for (String hash : dir.map().keySet()) {
                if (!hashes.contains(hash)) {
                    toDelete.add(hash);
                }
            }
            for (String s : toDelete) {
                dir.remove(s);
            }
            if (dir.map().isEmpty()) {
                toDeleteDirs.add(entry.getKey());
            }
        }
        for (String s : toDeleteDirs) {
            objects.remove(s);
        }
    }

    public static class AssetIndex {
        @LauncherNetworkAPI
        public boolean virtual;
        @LauncherNetworkAPI
        public Map<String, AssetIndexObject> objects;
    }

    public static class AssetIndexObject {
        @LauncherNetworkAPI
        public String hash;
        @LauncherNetworkAPI
        public long size;
    }
}
