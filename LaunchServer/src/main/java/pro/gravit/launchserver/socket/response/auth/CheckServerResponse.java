package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.events.request.CheckServerRequestEvent;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.core.interfaces.session.UserSessionSupportHardware;
import pro.gravit.launchserver.auth.core.interfaces.session.UserSessionSupportProperties;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.utils.HookException;

public class CheckServerResponse extends SimpleResponse {
    private transient final Logger logger = LogManager.getLogger();
    public String serverID;
    public String username;
    public boolean needHardware;
    public boolean needProperties;

    @Override
    public String getType() {
        return "checkServer";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client pClient) {
        if (pClient.permissions == null || !pClient.permissions.hasPerm("launchserver.checkserver")) {
            sendError("Permissions denied");
            return;
        }
        CheckServerRequestEvent result = new CheckServerRequestEvent();
        try {
            server.authHookManager.checkServerHook.hook(this, pClient);
            AuthManager.CheckServerReport report = server.authManager.checkServer(pClient, username, serverID);
            if (report == null) {
                sendError("User not verified");
                return;
            }
            result.playerProfile = report.playerProfile;
            result.uuid = report.uuid;
            if(pClient.permissions.hasPerm("launchserver.checkserver.extended") && report.session != null) {
                result.sessionId = report.session.getID();
                if(needProperties && report.session instanceof UserSessionSupportProperties supportProperties) {
                    result.sessionProperties = supportProperties.getProperties();
                }
                if(needHardware && report.session instanceof UserSessionSupportHardware supportHardware) {
                    result.hardwareId = supportHardware.getHardwareId();
                }
            }
            server.authHookManager.postCheckServerHook.hook(report, pClient);
            logger.debug("checkServer: {} uuid: {} serverID: {}", result.playerProfile == null ? null : result.playerProfile.username, result.uuid, serverID);
        } catch (AuthException | HookException e) {
            sendError(e.getMessage());
            return;
        } catch (Exception e) {
            logger.error("Internal authHandler error", e);
            sendError("Internal authHandler error");
            return;
        }
        sendResult(result);
    }
}
