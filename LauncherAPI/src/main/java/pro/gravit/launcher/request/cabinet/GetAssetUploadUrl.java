package pro.gravit.launcher.request.cabinet;

import pro.gravit.launcher.events.request.GetAssetUploadUrlRequestEvent;
import pro.gravit.launcher.request.Request;

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
