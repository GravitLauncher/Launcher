package pro.gravit.launchserver.auth.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.request.auth.AuthRequest;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MergeAuthCoreProvider extends AuthCoreProvider {
    private transient final Logger logger = LogManager.getLogger(MergeAuthCoreProvider.class);
    public List<String> list = new ArrayList<>();
    private final transient List<AuthCoreProvider> providers = new ArrayList<>();
    @Override
    public User getUserByUsername(String username) {
        for(var core : providers) {
            var result = core.getUserByUsername(username);
            if(result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public User getUserByUUID(UUID uuid) {
        for(var core : providers) {
            var result = core.getUserByUUID(uuid);
            if(result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws OAuthAccessTokenExpired {
        throw new OAuthAccessTokenExpired(); // Authorization not supported
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        return null;
    }

    @Override
    public AuthManager.AuthReport authorize(String login, AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password, boolean minecraftAccess) throws IOException {
        throw new AuthException("Authorization not supported");
    }

    @Override
    public User checkServer(Client client, String username, String serverID) throws IOException {
        for(var core : providers) {
            var result = core.checkServer(client, username, serverID);
            if(result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public boolean joinServer(Client client, String username, UUID uuid, String accessToken, String serverID) {
        return false; // Authorization not supported
    }

    @Override
    public void init(LaunchServer server, AuthProviderPair pair1) {
        for(var e : list) {
            var pair = server.config.auth.get(e);
            if(pair != null) {
                providers.add(pair.core);
            } else {
                logger.warn("Provider {} not found", e);
            }
        }
    }

    @Override
    public void close() {
        // Providers closed automatically
    }
}
