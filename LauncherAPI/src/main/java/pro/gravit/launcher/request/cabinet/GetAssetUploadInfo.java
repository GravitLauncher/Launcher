package pro.gravit.launcher.request.cabinet;

import pro.gravit.launcher.events.request.GetAssetUploadInfoRequestEvent;
import pro.gravit.launcher.request.Request;

public class GetAssetUploadInfo extends Request<GetAssetUploadInfoRequestEvent> {
    public String name;

    public GetAssetUploadInfo() {
    }

    public GetAssetUploadInfo(String name) {
        this.name = name;
    }

    @Override
    public String getType() {
        return "getAssetUploadUrl";
    }
}
