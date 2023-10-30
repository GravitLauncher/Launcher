package pro.gravit.launcher.request.cabinet;

import pro.gravit.launcher.events.request.AssetUploadInfoRequestEvent;
import pro.gravit.launcher.request.Request;

public class AssetUploadInfoRequest extends Request<AssetUploadInfoRequestEvent> {
    @Override
    public String getType() {
        return "assetUploadInfo";
    }
}
