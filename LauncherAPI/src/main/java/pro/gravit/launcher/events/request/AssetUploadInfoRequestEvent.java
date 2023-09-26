package pro.gravit.launcher.events.request;

import pro.gravit.launcher.events.RequestEvent;

public class AssetUploadInfoRequestEvent extends RequestEvent {
    public boolean uploadSkin;
    public boolean uploadCape;
    public SlimSupportConf slimSupportConf;

    public AssetUploadInfoRequestEvent(boolean uploadSkin, boolean uploadCape, SlimSupportConf slimSupportConf) {
        this.uploadSkin = uploadSkin;
        this.uploadCape = uploadCape;
        this.slimSupportConf = slimSupportConf;
    }

    @Override
    public String getType() {
        return "assetUploadInfo";
    }

    public enum SlimSupportConf {
        UNSUPPORTED, USER, SERVER
    }
}
