package pro.gravit.launcher.core.backend.extensions;

import pro.gravit.launcher.core.api.features.TextureUploadFeatureAPI;
import pro.gravit.launcher.core.api.model.Texture;

import java.util.concurrent.CompletableFuture;

public interface TextureUploadExtension extends Extension {
    CompletableFuture<TextureUploadFeatureAPI.TextureUploadInfo> fetchTextureUploadInfo();
    CompletableFuture<Texture> uploadTexture(String name, byte[] bytes, TextureUploadFeatureAPI.UploadSettings settings);
}
