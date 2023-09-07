package pro.gravit.launchserver.auth.core.interfaces.provider;

import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launchserver.auth.Feature;
import pro.gravit.launchserver.auth.core.User;

@Feature("assetupload")
public interface AuthSupportAssetUpload {
    String getAssetUploadUrl(String name, User user);

    default AuthRequestEvent.OAuthRequestEvent getAssetUploadToken(String name, User user) {
        return null;
    }
}
