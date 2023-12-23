package pro.gravit.launcher.base.events.request;

import pro.gravit.launcher.base.events.RequestEvent;

public class GetAssetUploadUrlRequestEvent extends RequestEvent {
    public static final String FEATURE_NAME = "assetupload";
    public String url;
    public AuthRequestEvent.OAuthRequestEvent token;

    public GetAssetUploadUrlRequestEvent() {
    }

    public GetAssetUploadUrlRequestEvent(String url, AuthRequestEvent.OAuthRequestEvent token) {
        this.url = url;
        this.token = token;
    }

    @Override
    public String getType() {
        return "getAssetUploadUrl";
    }
}
