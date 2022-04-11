package pro.gravit.launchserver.auth.protect;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.events.request.GetSecureLevelInfoRequestEvent;
import pro.gravit.launcher.events.request.HardwareReportRequestEvent;
import pro.gravit.launcher.events.request.VerifySecureLevelKeyRequestEvent;
import pro.gravit.launcher.request.secure.HardwareReportRequest;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.core.interfaces.UserHardware;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportHardware;
import pro.gravit.launchserver.auth.protect.interfaces.HardwareProtectHandler;
import pro.gravit.launchserver.auth.protect.interfaces.JoinServerProtectHandler;
import pro.gravit.launchserver.auth.protect.interfaces.SecureProtectHandler;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.launchserver.socket.response.auth.RestoreResponse;
import pro.gravit.launchserver.socket.response.secure.HardwareReportResponse;

import java.util.Base64;
import java.util.Date;

public class AdvancedProtectHandler extends StdProtectHandler implements SecureProtectHandler, HardwareProtectHandler, JoinServerProtectHandler {
    private transient final Logger logger = LogManager.getLogger();
    public boolean enableHardwareFeature;
    private transient LaunchServer server;

    @Override
    public boolean allowGetAccessToken(AuthResponse.AuthContext context) {
        return (context.authType == AuthResponse.ConnectTypes.CLIENT) && context.client.checkSign;
    }

    @Override
    public void checkLaunchServerLicense() {

    }

    @Override
    public GetSecureLevelInfoRequestEvent onGetSecureLevelInfo(GetSecureLevelInfoRequestEvent event) {
        return event;
    }

    @Override
    public boolean allowGetSecureLevelInfo(Client client) {
        return client.checkSign;
    }

    @Override
    public void onHardwareReport(HardwareReportResponse response, Client client) {
        if (!enableHardwareFeature) {
            response.sendResult(new HardwareReportRequestEvent(null));
            return;
        }
        if (!client.isAuth || client.trustLevel == null || client.trustLevel.publicKey == null) {
            response.sendError("Access denied");
            return;
        }
        logger.debug("HardwareInfo received");
        {
            var authSupportHardware = client.auth.isSupport(AuthSupportHardware.class);
            if (authSupportHardware != null) {
                UserHardware hardware = authSupportHardware.getHardwareInfoByData(response.hardware);
                if (hardware == null) {
                    hardware = authSupportHardware.createHardwareInfo(response.hardware, client.trustLevel.publicKey);
                } else {
                    authSupportHardware.addPublicKeyToHardwareInfo(hardware, client.trustLevel.publicKey);
                }
                authSupportHardware.connectUserAndHardware(client.sessionObject, hardware);
                if (hardware.isBanned()) {
                    throw new SecurityException("Your hardware banned");
                }
                client.trustLevel.hardwareInfo = hardware.getHardwareInfo();
                response.sendResult(new HardwareReportRequestEvent(createHardwareToken(client.username, hardware)));
                return;
            } else {
                logger.error("AuthCoreProvider not supported hardware");
                response.sendError("AuthCoreProvider not supported hardware");
                return;
            }
        }
    }

    @Override
    public VerifySecureLevelKeyRequestEvent onSuccessVerify(Client client) {
        if (enableHardwareFeature) {
            var authSupportHardware = client.auth.isSupport(AuthSupportHardware.class);
            if (authSupportHardware != null) {
                UserHardware hardware = authSupportHardware.getHardwareInfoByPublicKey(client.trustLevel.publicKey);
                if (hardware == null) //HWID not found?
                    return new VerifySecureLevelKeyRequestEvent(true, false, createPublicKeyToken(client.username, client.trustLevel.publicKey));
                if (hardware.isBanned()) {
                    throw new SecurityException("Your hardware banned");
                }
                client.trustLevel.hardwareInfo = hardware.getHardwareInfo();
                authSupportHardware.connectUserAndHardware(client.sessionObject, hardware);
                return new VerifySecureLevelKeyRequestEvent(false, false, createPublicKeyToken(client.username, client.trustLevel.publicKey), createHardwareToken(client.username, hardware));
            } else {
                logger.warn("AuthCoreProvider not supported hardware. HardwareInfo not checked!");
            }
        }
        return new VerifySecureLevelKeyRequestEvent(false, false, createPublicKeyToken(client.username, client.trustLevel.publicKey));
    }

    @Override
    public boolean onJoinServer(String serverID, String username, Client client) {
        return !enableHardwareFeature || (client.trustLevel != null && client.trustLevel.hardwareInfo != null);
    }

    @Override
    public void init(LaunchServer server) {
        this.server = server;
    }

    @Override
    public void close() {
    }

    public String createHardwareToken(String username, UserHardware hardware) {
        return Jwts.builder()
                .setIssuer("LaunchServer")
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 8))
                .claim("hardware", hardware.getId())
                .signWith(server.keyAgreementManager.ecdsaPrivateKey)
                .compact();
    }

    public String createPublicKeyToken(String username, byte[] publicKey) {
        return Jwts.builder()
                .setIssuer("LaunchServer")
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 8))
                .claim("publicKey", Base64.getEncoder().encodeToString(publicKey))
                .signWith(server.keyAgreementManager.ecdsaPrivateKey)
                .compact();
    }

    public static class HardwareInfoTokenVerifier implements RestoreResponse.ExtendedTokenProvider {
        private transient final LaunchServer server;
        private transient final Logger logger = LogManager.getLogger();
        private final JwtParser parser;

        public HardwareInfoTokenVerifier(LaunchServer server) {
            this.server = server;
            this.parser = Jwts.parserBuilder()
                    .requireIssuer("LaunchServer")
                    .setSigningKey(server.keyAgreementManager.ecdsaPublicKey)
                    .build();
        }

        @Override
        public boolean accept(Client client, AuthProviderPair pair, String extendedToken) {
            try {
                var parse = parser.parseClaimsJws(extendedToken);
                String hardwareInfoId = parse.getBody().get("hardware", String.class);
                if (hardwareInfoId == null) return false;
                if(client.auth == null) return false;
                var hardwareSupport = client.auth.core.isSupport(AuthSupportHardware.class);
                if(hardwareSupport == null) return false;
                UserHardware hardware = hardwareSupport.getHardwareInfoById(hardwareInfoId);
                if (client.trustLevel == null) client.trustLevel = new Client.TrustLevel();
                client.trustLevel.hardwareInfo = hardware.getHardwareInfo();
                return true;
            } catch (Throwable e) {
                logger.error("Hardware JWT error", e);
            }

            return false;
        }
    }

    public static class PublicKeyTokenVerifier implements RestoreResponse.ExtendedTokenProvider {
        private transient final LaunchServer server;
        private transient final Logger logger = LogManager.getLogger();
        private final JwtParser parser;

        public PublicKeyTokenVerifier(LaunchServer server) {
            this.server = server;
            this.parser = Jwts.parserBuilder()
                    .requireIssuer("LaunchServer")
                    .setSigningKey(server.keyAgreementManager.ecdsaPublicKey)
                    .build();
        }

        @Override
        public boolean accept(Client client, AuthProviderPair pair, String extendedToken) {
            try {
                var parse = parser.parseClaimsJws(extendedToken);
                String publicKey = parse.getBody().get("publicKey", String.class);
                if (publicKey == null) return false;
                if (client.trustLevel == null) client.trustLevel = new Client.TrustLevel();
                client.trustLevel.publicKey = Base64.getDecoder().decode(publicKey);
                return true;
            } catch (Throwable e) {
                logger.error("Public Key JWT error", e);
            }

            return false;
        }
    }
}
