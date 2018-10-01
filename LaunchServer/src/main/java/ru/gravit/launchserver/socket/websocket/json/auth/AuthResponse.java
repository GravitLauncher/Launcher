package ru.gravit.launchserver.socket.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.auth.hwid.HWID;
import ru.gravit.launchserver.auth.hwid.HWIDException;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.launchserver.socket.websocket.WebSocketFrameHandler;
import ru.gravit.launchserver.socket.websocket.json.JsonResponse;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.util.Collection;

public class AuthResponse implements JsonResponse {
    public String login;
    public String client;
    private LaunchServer server = LaunchServer.server;

    public String password;

    public AuthResponse(String login, String password, int authid, HWID hwid) {
        this.login = login;
        this.password = password;
        this.authid = authid;
        this.hwid = hwid;
    }

    public int authid;
    public HWID hwid;
    @Override
    public String getType() {
        return "auth";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        try {
            String ip = IOHelper.getIP(ctx.channel().remoteAddress());
            if (server.limiter.isLimit(ip)) {
                AuthProvider.authError(server.config.authRejectString);
                return;
            }
            AuthProvider provider = server.config.authProvider[authid];
            AuthProviderResult result = provider.auth(login, password, ip);
            if (!VerifyHelper.isValidUsername(result.username)) {
                AuthProvider.authError(String.format("Illegal result: '%s'", result.username));
                return;
            }
            Collection<SignedObjectHolder<ClientProfile>> profiles = server.getProfiles();
            for (SignedObjectHolder<ClientProfile> p : profiles) {
                if (p.object.getTitle().equals(client)) {
                    if (!p.object.isWhitelistContains(login)) {
                        throw new AuthException(server.config.whitelistRejectString);
                    }
                    //clientData.profile = p.object;
                }
            }
            //if(clientData.profile == null) {
            //    throw new AuthException("You profile not found");
            //}
            server.config.hwidHandler.check(hwid, result.username);
        } catch (AuthException | HWIDException e)
        {
            ctx.channel().writeAndFlush(new TextWebSocketFrame(WebSocketFrameHandler.gson.toJson(new ErrorResult(e.getMessage()))));
        }
    }
    public class ErrorResult
    {
        public ErrorResult(String error) {
            this.error = error;
        }

        public String error;
    }
}
