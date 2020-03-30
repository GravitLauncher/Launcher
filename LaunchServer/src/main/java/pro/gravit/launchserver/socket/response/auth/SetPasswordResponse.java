package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.request.SetPasswordRequestEvent;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class SetPasswordResponse extends SimpleResponse {
    public String oldPassword;
    public String newPassword;
    public String username;

    @Override
    public String getType() {
        return "setPassword";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        if ((oldPassword == null && username == null) || newPassword == null) {
            sendError("Request invalid");
            return;
        }
        if (!client.isAuth) {
            sendError("You not authorized");
            return;
        }
        if (username != null && !client.permissions.isPermission(ClientPermissions.PermissionConsts.ADMIN)) {
            sendError("You not admin");
            return;
        }
        if (username != null) {
            User user = server.config.dao.userDAO.findByUsername(username);
            user.setPassword(newPassword);
            sendResult(new SetPasswordRequestEvent());
        } else {
            User user = server.config.dao.userDAO.findByUsername(client.username);
            if (user.verifyPassword(oldPassword)) {
                user.setPassword(newPassword);
                sendResult(new SetPasswordRequestEvent());
            } else {
                sendError("Old password incorrect");
            }
        }
    }
}
