package pro.gravit.launchserver.socket.response.secure;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.VerifySecureLevelKeyRequestEvent;
import pro.gravit.launchserver.auth.protect.interfaces.SecureProtectHandler;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

public class VerifySecureLevelKeyResponse extends SimpleResponse {
    public byte[] publicKey;
    public byte[] signature;
    @Override
    public String getType() {
        return "verifySecureLevelKey";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        if(!(server.config.protectHandler instanceof SecureProtectHandler))
        {
            sendError("This method not allowed");
            return;
        }
        SecureProtectHandler secureProtectHandler = (SecureProtectHandler) server.config.protectHandler;
        try {
            secureProtectHandler.verifySecureLevelKey(publicKey, signature);
        } catch (InvalidKeySpecException e)
        {
            sendError("Invalid public key");
            return;
        } catch (SignatureException e)
        {
            sendError("Invalid signature");
            return;
        }
        sendResult(new VerifySecureLevelKeyRequestEvent());
    }
}
