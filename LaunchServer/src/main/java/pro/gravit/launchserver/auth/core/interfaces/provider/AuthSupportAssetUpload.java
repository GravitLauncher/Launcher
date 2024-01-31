package pro.gravit.launchserver.auth.core.interfaces.provider;

import pro.gravit.launcher.base.events.request.AssetUploadInfoRequestEvent;
import pro.gravit.launcher.base.events.request.AuthRequestEvent;
import pro.gravit.launcher.base.events.request.GetAssetUploadUrlRequestEvent;
import pro.gravit.launchserver.auth.Feature;
import pro.gravit.launchserver.auth.core.User;

import java.util.Set;

@Feature(GetAssetUploadUrlRequestEvent.FEATURE_NAME)
public interface AuthSupportAssetUpload extends AuthSupport {
    String getAssetUploadUrl(String name, User user);

    default AuthRequestEvent.OAuthRequestEvent getAssetUploadToken(String name, User user) {
        return null;
    }

    default AssetUploadInfoRequestEvent getAssetUploadInfo(User user) {
        return new AssetUploadInfoRequestEvent(Set.of("SKIN", "CAPE"), AssetUploadInfoRequestEvent.SlimSupportConf.USER);
    }
}
