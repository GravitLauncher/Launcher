package pro.gravit.launcher.core.api.features;

import pro.gravit.launcher.core.api.model.Texture;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface TextureUploadFeatureAPI {
    CompletableFuture<TextureUploadInfo> fetchInfo();
    CompletableFuture<Texture> upload(String name, byte[] bytes, UploadSettings settings);

    interface TextureUploadInfo {
        Set<String> getAvailable();
        boolean isRequireManualSlimSkinSelect();
    }

    record UploadSettings(boolean slim) {

    }
}
