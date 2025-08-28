package pro.gravit.launchserver.socket.response.update;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.events.request.LauncherRequestEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.updates.UpdatesProvider;
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
        UpdatesProvider.UpdateVariant variant = UpdatesProvider.UpdateVariant.JAR;
        if(launcher_type == 2) {
            variant = UpdatesProvider.UpdateVariant.EXE;
        }
        byte[] hashToCheck = bytes;
        UpdatesProvider.UpdateInfo info = server.config.updatesProvider.checkUpdates(variant, new UpdatesProvider.BuildSecretsCheck(secureHash, secureSalt, hashToCheck));
        if (info != null) {
            sendResult(new LauncherRequestEvent(true, info.url()));
        } else {
            client.checkSign = true;
            sendResult(new LauncherRequestEvent(false, null, createLauncherExtendedToken(), server.config.netty.security.launcherTokenExpire*1000));
        }
    }

    public String createLauncherExtendedToken() {
        return Jwts.builder()
                .setIssuer("LaunchServer")
                .claim("checkSign", true)
                .setExpiration(Date.from(LocalDateTime.now().plusSeconds(server.config.netty.security.launcherTokenExpire).toInstant(ZoneOffset.UTC)))
                .signWith(server.keyAgreementManager.ecdsaPrivateKey, SignatureAlgorithm.ES256)
                .compact();
    }

    public static class LauncherTokenVerifier implements RestoreResponse.ExtendedTokenProvider {
        private final JwtParser parser;
        private final Logger logger = LogManager.getLogger();

        public LauncherTokenVerifier(LaunchServer server) {
            parser = Jwts.parser()
                    .verifyWith(server.keyAgreementManager.ecdsaPublicKey)
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

    @Override
    public ThreadSafeStatus getThreadSafeStatus() {
        return ThreadSafeStatus.READ_WRITE;
    }

}
