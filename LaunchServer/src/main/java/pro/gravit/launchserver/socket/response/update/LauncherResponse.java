package pro.gravit.launchserver.socket.response.update;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.events.request.LauncherRequestEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.launchserver.socket.response.auth.RestoreResponse;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.SecurityHelper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

public class LauncherResponse extends SimpleResponse {
    public Version version;
    public String hash;
    public byte[] digest;
    public int launcher_type;

    public String secureHash;
    public String secureSalt;

    @Override
    public String getType() {
        return "launcher";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        byte[] bytes;
        if (hash != null)
            bytes = Base64.getDecoder().decode(hash);
        else
            bytes = digest;
        if (launcher_type == 1) // JAR
        {
            byte[] hash = server.launcherBinary.getDigest();
            if (hash == null)
                service.sendObjectAndClose(ctx, new LauncherRequestEvent(true, server.config.netty.launcherURL));
            if (Arrays.equals(bytes, hash) && checkSecure(secureHash, secureSalt)) {
                client.checkSign = true;
                sendResult(new LauncherRequestEvent(false, server.config.netty.launcherURL, createLauncherExtendedToken()));
            } else {
                sendResultAndClose(new LauncherRequestEvent(true, server.config.netty.launcherURL, createLauncherExtendedToken()));
            }
        } else if (launcher_type == 2) //EXE
        {
            byte[] hash = server.launcherEXEBinary.getDigest();
            if (hash == null) sendResultAndClose(new LauncherRequestEvent(true, server.config.netty.launcherEXEURL));
            if (Arrays.equals(bytes, hash) && checkSecure(secureHash, secureSalt)) {
                client.checkSign = true;
                sendResult(new LauncherRequestEvent(false, server.config.netty.launcherEXEURL, createLauncherExtendedToken()));
            } else {
                sendResultAndClose(new LauncherRequestEvent(true, server.config.netty.launcherEXEURL, createLauncherExtendedToken()));
            }
        } else sendError("Request launcher type error");
    }

    public String createLauncherExtendedToken() {
        return Jwts.builder()
                .setIssuer("LaunchServer")
                .claim("checkSign", true)
                .setExpiration(Date.from(LocalDateTime.now().plusHours(8).toInstant(ZoneOffset.UTC)))
                .signWith(server.keyAgreementManager.ecdsaPrivateKey, SignatureAlgorithm.ES256)
                .compact();
    }

    private boolean checkSecure(String hash, String salt) {
        if (hash == null || salt == null) return false;
        byte[] normal_hash = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256,
                server.runtime.clientCheckSecret.concat(".").concat(salt));
        byte[] launcher_hash = Base64.getDecoder().decode(hash);
        return Arrays.equals(normal_hash, launcher_hash);
    }

    public static class LauncherTokenVerifier implements RestoreResponse.ExtendedTokenProvider {
        private final LaunchServer server;
        private final JwtParser parser;
        private final Logger logger = LogManager.getLogger();

        public LauncherTokenVerifier(LaunchServer server) {
            this.server = server;
            parser = Jwts.parserBuilder()
                    .setSigningKey(server.keyAgreementManager.ecdsaPublicKey)
                    .requireIssuer("LaunchServer")
                    .build();
        }

        @Override
        public boolean accept(Client client, AuthProviderPair pair, String extendedToken) {
            try {
                var jwt = parser.parseClaimsJws(extendedToken);
                client.checkSign = jwt.getBody().get("checkSign", Boolean.class);
                client.type = AuthResponse.ConnectTypes.CLIENT;
                return true;
            } catch (Exception e) {
                logger.error("JWT check failed", e);
                return false;
            }

        }
    }

}
