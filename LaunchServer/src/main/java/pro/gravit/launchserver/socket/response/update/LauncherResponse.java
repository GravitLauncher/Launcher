package pro.gravit.launchserver.socket.response.update;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.LauncherRequestEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.SecurityHelper;

import java.util.Arrays;
import java.util.Base64;

public class LauncherResponse extends SimpleResponse {
    public Version version;
    public String hash;
    public byte[] digest;
    public int launcher_type;

    public String secureHash;
    public String secureSalt;

    @Override
    public String getType() {
        return "launcher";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        byte[] bytes;
        if (hash != null)
            bytes = Base64.getDecoder().decode(hash);
        else
            bytes = digest;
        if (launcher_type == 1) // JAR
        {
            byte[] hash = server.launcherBinary.getDigest();
            if (hash == null)
                service.sendObjectAndClose(ctx, new LauncherRequestEvent(true, server.config.netty.launcherURL));
            if (Arrays.equals(bytes, hash) && checkSecure(secureHash, secureSalt)) {
                client.checkSign = true;
                sendResult(new LauncherRequestEvent(false, server.config.netty.launcherURL));
            } else {
                sendResultAndClose(new LauncherRequestEvent(true, server.config.netty.launcherURL));
            }
        } else if (launcher_type == 2) //EXE
        {
            byte[] hash = server.launcherEXEBinary.getDigest();
            if (hash == null) sendResultAndClose(new LauncherRequestEvent(true, server.config.netty.launcherEXEURL));
            if (Arrays.equals(bytes, hash) && checkSecure(secureHash, secureSalt)) {
                client.checkSign = true;
                sendResult(new LauncherRequestEvent(false, server.config.netty.launcherEXEURL));
            } else {
                sendResultAndClose(new LauncherRequestEvent(true, server.config.netty.launcherEXEURL));
            }
        } else sendError("Request launcher type error");
    }

    private boolean checkSecure(String hash, String salt) {
        if (hash == null || salt == null) return false;
        byte[] normal_hash = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256,
                server.runtime.clientCheckSecret.concat(".").concat(salt));
        byte[] launcher_hash = Base64.getDecoder().decode(hash);
        return Arrays.equals(normal_hash, launcher_hash);
    }

}
