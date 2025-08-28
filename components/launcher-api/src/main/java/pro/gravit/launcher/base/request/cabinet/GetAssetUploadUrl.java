package pro.gravit.launcher.base.request.cabinet;

import pro.gravit.launcher.base.events.request.GetAssetUploadUrlRequestEvent;
import pro.gravit.launcher.base.request.Request;

public class GetAssetUploadUrl extends Request<GetAssetUploadUrlRequestEvent> {
    public String name;

    public GetAssetUploadUrl() {
    }

    public GetAssetUploadUrl(String name) {
        this.name = name;
    }

    @Override
    public String getType() {
        return "getAssetUploadUrl";
    }
}
