package pro.gravit.launcher.base.events.request;

import pro.gravit.launcher.base.events.RequestEvent;
import pro.gravit.launcher.core.api.features.TextureUploadFeatureAPI;

import java.util.Set;

public class AssetUploadInfoRequestEvent extends RequestEvent implements TextureUploadFeatureAPI.TextureUploadInfo {
    public Set<String> available;
    public SlimSupportConf slimSupportConf;

    public AssetUploadInfoRequestEvent(Set<String> available, SlimSupportConf slimSupportConf) {
        this.available = available;
        this.slimSupportConf = slimSupportConf;
    }

    @Override
    public String getType() {
        return "assetUploadInfo";
    }

    @Override
    public Set<String> getAvailable() {
        return available;
    }

    @Override
    public boolean isRequireManualSlimSkinSelect() {
        return slimSupportConf == SlimSupportConf.USER;
    }

    public enum SlimSupportConf {
        UNSUPPORTED, USER, SERVER
    }
}
