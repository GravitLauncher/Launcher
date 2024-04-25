package pro.gravit.launchserver.auth.core.openid;

import java.net.URI;

public record OpenIDConfig(URI tokenUri, String authorizationEndpoint, String clientId, String clientSecret,
                           String redirectUri, URI jwksUri, String scopes, String issuer,
                           ClaimExtractorConfig extractorConfig) {

    public record ClaimExtractorConfig(String usernameClaim, String uuidClaim) {}
}
