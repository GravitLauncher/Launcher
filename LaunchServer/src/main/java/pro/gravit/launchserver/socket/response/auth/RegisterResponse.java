package pro.gravit.launchserver.socket.response.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class RegisterResponse extends SimpleResponse {
    public String login;
    public String password;
    public String email;
    public byte[] verifyHash;
    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception
    {
        byte[] normalHash = registerHash(login, server.runtime.registerApiKey);
        if(!( client.isAuth && client.permissions.canAdmin ) && !Arrays.equals(normalHash, verifyHash))
        {
            sendError("Hash invalid");
            return;
        }
        User checkUser = server.config.dao.userService.findUserByUsername(login);
        if(checkUser != null)
        {
            sendError("User already register");
            return;
        }
        User user = new User();
        user.username = login;
        user.email = email;
        user.setPassword(password);
        user.uuid = UUID.randomUUID();
        server.config.dao.userService.saveUser(user);
    }
    @Override
    public String getType() {
        return "register";
    }
    public static byte[] registerHash(String login, String secret) throws NoSuchAlgorithmException {
        String text = login.concat("+").concat(secret);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(text.getBytes(StandardCharsets.UTF_8));
    }
}
