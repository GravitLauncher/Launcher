package ru.gravit.launchserver.socket.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.auth.hwid.HWID;
import ru.gravit.launchserver.auth.hwid.HWIDException;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.util.Collection;

public class AuthResponse implements JsonResponseInterface {
    public String login;
    public String client;

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
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client clientData) throws Exception {
        try {
            String ip = IOHelper.getIP(ctx.channel().remoteAddress());
            if (LaunchServer.server.limiter.isLimit(ip)) {
                AuthProvider.authError(LaunchServer.server.config.authRejectString);
                return;
            }
            if (!clientData.checkSign) {
                AuthProvider.authError("Don't skip Launcher Update");
                return;
            }
            AuthProvider provider = LaunchServer.server.config.authProvider[authid];
            AuthProviderResult result = provider.auth(login, password, ip);
            if (!VerifyHelper.isValidUsername(result.username)) {
                AuthProvider.authError(String.format("Illegal result: '%s'", result.username));
                return;
            }
            Collection<SignedObjectHolder<ClientProfile>> profiles = LaunchServer.server.getProfiles();
            for (SignedObjectHolder<ClientProfile> p : profiles) {
                if (p.object.getTitle().equals(client)) {
                    if (!p.object.isWhitelistContains(login)) {
                        throw new AuthException(LaunchServer.server.config.whitelistRejectString);
                    }
                    clientData.profile = p.object;
                }
            }
            if (clientData.profile == null) {
                throw new AuthException("You profile not found");
            }
            LaunchServer.server.config.hwidHandler.check(hwid, result.username);
            clientData.isAuth = true;
            clientData.permissions = result.permissions;
            service.sendObject(ctx, new WebSocketService.SuccessResult("auth"));
        } catch (AuthException | HWIDException e) {
            service.sendObject(ctx, new WebSocketService.ErrorResult(e.getMessage()));
        }
    }

    public class Result {
        public Result() {
        }

        public String error;
    }
}
