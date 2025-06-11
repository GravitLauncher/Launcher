package pro.gravit.launchserver.helper;

import com.google.gson.JsonObject;
import pro.gravit.launcher.base.Downloader;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launchserver.HttpRequester;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AssetsDirHelper {
    public static final String MINECRAFT_VERSIONS_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    public static final String RESOURCES_DOWNLOAD_URL = "https://resources.download.minecraft.net/";

    public static List<Downloader.SizedFile> makeToDownloadFiles(AssetInfo assetInfo, HashedDir updatesDir) {
        List<Downloader.SizedFile> toDownload = new ArrayList<>(128);
        for (var e : assetInfo.assets.entrySet()) {
            var value = e.getValue().getAsJsonObject();
            var hash = value.get("hash").getAsString();
            hash = hash.substring(0, 2) + "/" + hash;
            var size = value.get("size").getAsLong();
            var path = "objects/" + hash;
            if (updatesDir.tryFindRecursive(path).isFound()) {
                continue;
            }
            toDownload.add(new Downloader.SizedFile(hash, path, size));
        }
        return toDownload;
    }

    public static AssetInfo getAssetInfo(HttpRequester requester, String versionName) throws IOException {
        var versions = requester.send(requester.get(MINECRAFT_VERSIONS_URL, null), MinecraftVersions.class).getOrThrow();
        String profileUrl = null;
        for (var e : versions.versions) {
            if (e.id.equals(versionName)) {
                profileUrl = e.url;
                break;
            }
        }
        var profileInfo = requester.send(requester.get(profileUrl, null), MiniVersion.class).getOrThrow();
        String assetsIndexUrl = profileInfo.assetIndex.url;
        String assetIndex = profileInfo.assetIndex.id;
        JsonObject assets = requester.send(requester.get(assetsIndexUrl, null), JsonObject.class).getOrThrow();
        return new AssetInfo(assetIndex, assets);
    }

    public record AssetInfo(String assetIndex, JsonObject assets) {

    }

    public record MiniVersionInfo(String id, String url) {

    }

    public record MinecraftVersions(List<MiniVersionInfo> versions) {

    }

    public record MinecraftAssetIndexInfo(String id, String url) {

    }

    public record MiniVersion(MinecraftAssetIndexInfo assetIndex) {

    }
}
