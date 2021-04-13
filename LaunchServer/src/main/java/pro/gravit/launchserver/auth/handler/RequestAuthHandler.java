package pro.gravit.launchserver.auth.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public final class RequestAuthHandler extends CachedAuthHandler {
    private transient final Logger logger = LogManager.getLogger();
    private final String splitSymbol = ":";
    private final String goodResponse = "OK";
    private String usernameFetch;
    private String uuidFetch;
    private String updateAuth;
    private String updateServerID;

    @Override
    public void init(LaunchServer srv) {
        super.init(srv);
        if (usernameFetch == null)
            logger.error("usernameFetch cannot be null");
        if (uuidFetch == null)
            logger.error("uuidFetch cannot be null");
        if (updateAuth == null)
            logger.error("updateAuth cannot be null");
        if (updateServerID == null)
            logger.error("updateServerID cannot be null");
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        String response = IOHelper.request(new URL(CommonHelper.replace(uuidFetch, "uuid", IOHelper.urlEncode(uuid.toString()))));
        String[] parts = response.split(splitSymbol);
        String username = parts[0];
        String accessToken = parts[1];
        String serverID = parts[2];
        if (logger.isDebugEnabled()) {
            logger.debug("[AuthHandler] Got username: " + username);
            logger.debug("[AuthHandler] Got accessToken: " + accessToken);
            logger.debug("[AuthHandler] Got serverID: " + serverID);
            logger.debug("[AuthHandler] Got UUID: " + uuid);
        }
        return new Entry(uuid, username, accessToken, serverID);
    }

    @Override
    protected Entry fetchEntry(String username) throws IOException {
        String response = IOHelper.request(new URL(CommonHelper.replace(usernameFetch, "user", IOHelper.urlEncode(username))));
        String[] parts = response.split(splitSymbol);
        UUID uuid = UUID.fromString(parts[0]);
        String accessToken = parts[1];
        String serverID = parts[2];
        if (logger.isDebugEnabled()) {
            logger.debug("[AuthHandler] Got username: " + username);
            logger.debug("[AuthHandler] Got accessToken: " + accessToken);
            logger.debug("[AuthHandler] Got serverID: " + serverID);
            logger.debug("[AuthHandler] Got UUID: " + uuid);
        }
        return new Entry(uuid, username, accessToken, serverID);
    }

    @Override
    protected boolean updateAuth(UUID uuid, String username, String accessToken) throws IOException {
        String response = IOHelper.request(new URL(CommonHelper.replace(updateAuth, "user", IOHelper.urlEncode(username), "uuid", IOHelper.urlEncode(uuid.toString()), "token", IOHelper.urlEncode(accessToken))));
        if (logger.isDebugEnabled()) {
            logger.debug("[AuthHandler] Set accessToken: " + accessToken);
            logger.debug("[AuthHandler] Set UUID: " + uuid);
            logger.debug("[AuthHandler] For this username: " + username);
        }
        return goodResponse.equals(response);
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        String response = IOHelper.request(new URL(CommonHelper.replace(updateAuth, "serverid", IOHelper.urlEncode(serverID), "uuid", IOHelper.urlEncode(uuid.toString()))));
        if (logger.isDebugEnabled()) {
            logger.debug("[AuthHandler] Set serverID: " + serverID);
            logger.debug("[AuthHandler] For this UUID: " + uuid);
        }
        return goodResponse.equals(response);
    }

    @Override
    public void close() {
    }
}
