package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.events.request.CheckServerRequestEvent;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.launchserver.socket.response.profile.ProfileByUUIDResponse;
import pro.gravit.utils.HookException;
import pro.gravit.utils.helper.LogHelper;

public class CheckServerResponse extends SimpleResponse {
    public String serverID;
    public String username;
    public String client;
    private transient final Logger logger = LogManager.getLogger();

    @Override
    public String getType() {
        return "checkServer";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client pClient) {
        if (!pClient.isAuth || pClient.type == AuthResponse.ConnectTypes.CLIENT) {
            sendError("Permissions denied");
            return;
        }
        CheckServerRequestEvent result = new CheckServerRequestEvent();
        try {
            server.authHookManager.checkServerHook.hook(this, pClient);
            result.uuid = server.authManager.checkServer(pClient, username, serverID);
            if (result.uuid != null) {
                result.playerProfile = ProfileByUUIDResponse.getProfile(result.uuid, username, client, pClient.auth.textureProvider);
                logger.debug("checkServer: {} uuid: {} serverID: {}", result.playerProfile.username, result.uuid, serverID);
            }
        } catch (AuthException | HookException e) {
            sendError(e.getMessage());
            return;
        } catch (Exception e) {
            logger.error(e);
            sendError("Internal authHandler error");
            return;
        }
        sendResult(result);
    }

}
