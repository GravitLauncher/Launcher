package pro.gravit.launchserver.auth.core.openid;

import com.google.gson.annotations.SerializedName;

public record AccessTokenResponse(@SerializedName("access_token") String accessToken,
                                  @SerializedName("expires_in") Long expiresIn,
                                  @SerializedName("refresh_expires_in") Long refreshExpiresIn,
                                  @SerializedName("refresh_token") String refreshToken,
                                  @SerializedName("token_type") String tokenType,
                                  @SerializedName("id_token") String idToken,
                                  @SerializedName("not-before-policy") Integer notBeforePolicy,
                                  @SerializedName("session_state") String sessionState,
                                  @SerializedName("scope") String scope) {
}
