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
import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.core.interfaces.UserHardware;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportHardware;
import pro.gravit.launchserver.auth.protect.hwid.HWIDException;
import pro.gravit.launchserver.auth.protect.hwid.HWIDProvider;
import pro.gravit.launchserver.auth.protect.interfaces.HardwareProtectHandler;
import pro.gravit.launchserver.auth.protect.interfaces.JoinServerProtectHandler;
import pro.gravit.launchserver.auth.protect.interfaces.SecureProtectHandler;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.launchserver.socket.response.auth.RestoreResponse;
import pro.gravit.launchserver.socket.response.secure.HardwareReportResponse;
import pro.gravit.utils.command.Command;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AdvancedProtectHandler extends StdProtectHandler implements SecureProtectHandler, HardwareProtectHandler, JoinServerProtectHandler, Reconfigurable {
    private transient final Logger logger = LogManager.getLogger();
    public boolean enableHardwareFeature;
    public HWIDProvider provider;
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
            response.sendResult(new HardwareReportRequestEvent(createHardwareToken(client.username, response.hardware)));
            return;
        }
        try {
            if (!client.isAuth || client.trustLevel == null || client.trustLevel.publicKey == null) {
                response.sendError("Access denied");
                return;
            }
            logger.debug("HardwareInfo received");
            if (client.auth.core instanceof AuthSupportHardware) {
                AuthSupportHardware authSupportHardware = (AuthSupportHardware) client.auth;
                UserHardware hardware = authSupportHardware.getHardwareInfoByData(response.hardware);
                if (hardware == null) {
                    hardware = authSupportHardware.createHardwareInfo(response.hardware, client.trustLevel.publicKey);
                } else {
                    authSupportHardware.addPublicKeyToHardwareInfo(hardware, client.trustLevel.publicKey);
                }
                authSupportHardware.connectUserAndHardware(client.getUser(), hardware);
                if (hardware.isBanned()) {
                    throw new SecurityException("Your hardware banned");
                }
                client.trustLevel.hardwareInfo = hardware.getHardwareInfo();
            } else {
                provider.normalizeHardwareInfo(response.hardware);
                boolean needCreate = !provider.addPublicKeyToHardwareInfo(response.hardware, client.trustLevel.publicKey, client);
                logger.debug("HardwareInfo needCreate: {}", needCreate ? "true" : "false");
                if (needCreate)
                    provider.createHardwareInfo(response.hardware, client.trustLevel.publicKey, client);
                client.trustLevel.hardwareInfo = response.hardware;
            }
        } catch (HWIDException e) {
            throw new SecurityException(e.getMessage());
        }
        response.sendResult(new HardwareReportRequestEvent(createHardwareToken(client.username, response.hardware)));
    }

    @Override
    public VerifySecureLevelKeyRequestEvent onSuccessVerify(Client client) {
        if (enableHardwareFeature) {
            if (client.isAuth && client.auth.core instanceof AuthSupportHardware) {
                UserHardware hardware = ((AuthSupportHardware) client.auth.core).getHardwareInfoByPublicKey(client.trustLevel.publicKey);
                if (hardware == null) //HWID not found?
                    return new VerifySecureLevelKeyRequestEvent(true, false, createPublicKeyToken(client.username, client.trustLevel.publicKey));
                if (hardware.isBanned()) {
                    throw new SecurityException("Your hardware banned");
                }
                client.trustLevel.hardwareInfo = hardware.getHardwareInfo();
            } else if (provider == null) {
                logger.warn("HWIDProvider null. HardwareInfo not checked!");
            } else {
                try {
                    if (client.auth.core instanceof AuthSupportHardware) {
                        AuthSupportHardware authSupportHardware = (AuthSupportHardware) client.auth;
                        UserHardware hardware = authSupportHardware.getHardwareInfoByPublicKey(client.trustLevel.publicKey);
                        if (hardware != null) {
                            client.trustLevel.hardwareInfo = hardware.getHardwareInfo();
                            authSupportHardware.connectUserAndHardware(client.getUser(), hardware);
                        }
                    } else {
                        client.trustLevel.hardwareInfo = provider.findHardwareInfoByPublicKey(client.trustLevel.publicKey, client);
                    }
                    if (client.trustLevel.hardwareInfo == null) //HWID not found?
                        return new VerifySecureLevelKeyRequestEvent(true, false, createPublicKeyToken(client.username, client.trustLevel.publicKey));
                } catch (HWIDException e) {
                    throw new SecurityException(e.getMessage()); //Show banned message
                }
            }
            return new VerifySecureLevelKeyRequestEvent(false, false, createPublicKeyToken(client.username, client.trustLevel.publicKey));
        }
        return new VerifySecureLevelKeyRequestEvent(false, false, createPublicKeyToken(client.username, client.trustLevel.publicKey));
    }

    @Override
    public Map<String, Command> getCommands() {
        Map<String, Command> commands = new HashMap<>();
        if (provider instanceof Reconfigurable) {
            commands.putAll(((Reconfigurable) provider).getCommands());
        }
        return commands;
    }

    @Override
    public boolean onJoinServer(String serverID, String username, Client client) {
        return !enableHardwareFeature || (client.trustLevel != null && client.trustLevel.hardwareInfo != null);
    }

    @Override
    public void init(LaunchServer server) {
        if (provider != null)
            provider.init(server);
        this.server = server;
    }

    @Override
    public void close() {
        if (provider != null)
            provider.close();
    }

    public String createHardwareToken(String username, HardwareReportRequest.HardwareInfo info) {
        return Jwts.builder()
                .setIssuer("LaunchServer")
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 8))
                .claim("hardware", info)
                .signWith(server.keyAgreementManager.ecdsaPrivateKey)
                .compact();
    }

    public String createPublicKeyToken(String username, byte[] publicKey) {
        return Jwts.builder()
                .setIssuer("LaunchServer")
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 8))
                .claim("publicKey", Base64.getEncoder().encode(publicKey))
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
                HardwareReportRequest.HardwareInfo hardwareInfo = parse.getBody().get("hardware", HardwareReportRequest.HardwareInfo.class);
                if (hardwareInfo == null) return false;
                if (client.trustLevel == null) client.trustLevel = new Client.TrustLevel();
                client.trustLevel.hardwareInfo = hardwareInfo;
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
