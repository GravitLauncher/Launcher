package pro.gravit.launchserver.helper;

import io.jsonwebtoken.Jwts;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.utils.helper.SecurityHelper;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

public class LegacySessionHelper {
    public static String makeAccessJwtTokenFromString(User user, LocalDateTime expirationTime, ECPrivateKey privateKey) {
        return Jwts.builder()
                .setIssuer("LaunchServer")
                .setSubject(user.getUsername())
                .claim("uuid", user.getUUID().toString())
                .setExpiration(Date.from(expirationTime
                                .toInstant(ZoneOffset.UTC)))
                .signWith(privateKey)
                .compact();
    }

    public static JwtTokenInfo getJwtInfoFromAccessToken(String token, ECPublicKey publicKey) {
        var parser = Jwts.parserBuilder()
                .requireIssuer("LaunchServer")
                .setClock(() -> new Date(Clock.systemUTC().millis()))
                .setSigningKey(publicKey)
                .build();
        var claims = parser.parseClaimsJws(token);
        var uuid = UUID.fromString(claims.getBody().get("uuid", String.class));
        var username = claims.getBody().getSubject();
        return new JwtTokenInfo(username, uuid);
    }

    public static String makeRefreshTokenFromPassword(String username, String rawPassword, String secretSalt) {
        if(rawPassword == null) {
            rawPassword = "";
        }
        return SecurityHelper.toHex(SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256,
                String.format("%s.%s.%s.%s", secretSalt, username, rawPassword, secretSalt)));
    }

    public record JwtTokenInfo(String username, UUID uuid) {
    }
}
