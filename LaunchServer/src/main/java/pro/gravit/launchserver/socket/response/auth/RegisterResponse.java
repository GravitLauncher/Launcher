package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class RegisterResponse extends SimpleResponse {
    public String login;
    public String password;
    public String email;
    public byte[] verifyHash;

    public static byte[] registerHash(String login, String secret) throws NoSuchAlgorithmException {
        String text = login.concat("+").concat(secret);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(text.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        byte[] normalHash = registerHash(login, server.runtime.registerApiKey);
        if (!(client.isAuth && client.permissions.isPermission(ClientPermissions.PermissionConsts.ADMIN)) && !Arrays.equals(normalHash, verifyHash)) {
            sendError("Hash invalid");
            return;
        }
        User checkUser = server.config.dao.userDAO.findByUsername(login);
        if (checkUser != null) {
            sendError("User already register");
            return;
        }
    }

    @Override
    public String getType() {
        return "register";
    }
}
