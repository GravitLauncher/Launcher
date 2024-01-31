package pro.gravit.launchserver.auth.mix;

import pro.gravit.launcher.base.events.request.AssetUploadInfoRequestEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportAssetUpload;

import java.util.Map;

public class UploadAssetMixProvider extends MixProvider implements AuthSupportAssetUpload {
    public Map<String, String> urls;
    public AssetUploadInfoRequestEvent.SlimSupportConf slimSupportConf;

    @Override
    public String getAssetUploadUrl(String name, User user) {
        return urls.get(name);
    }

    @Override
    public AssetUploadInfoRequestEvent getAssetUploadInfo(User user) {
        return new AssetUploadInfoRequestEvent(urls.keySet(), slimSupportConf);
    }

    @Override
    public void init(LaunchServer server, AuthCoreProvider core) {

    }

    @Override
    public void close() {

    }
}
