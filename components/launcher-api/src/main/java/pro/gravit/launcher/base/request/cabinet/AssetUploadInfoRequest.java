package pro.gravit.launcher.base.request.cabinet;

import pro.gravit.launcher.base.events.request.AssetUploadInfoRequestEvent;
import pro.gravit.launcher.base.request.Request;

public class AssetUploadInfoRequest extends Request<AssetUploadInfoRequestEvent> {
    @Override
    public String getType() {
        return "assetUploadInfo";
    }
}
