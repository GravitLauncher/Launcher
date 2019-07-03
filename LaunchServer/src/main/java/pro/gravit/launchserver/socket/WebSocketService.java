package pro.gravit.launchserver.socket;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import com.google.gson.Gson;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.events.ExceptionEvent;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.events.request.ErrorRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.launcher.request.admin.ProxyRequest;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.response.WebSocketServerResponse;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.launchserver.socket.response.admin.AddLogListenerResponse;
import pro.gravit.launchserver.socket.response.admin.ExecCommandResponse;
import pro.gravit.launchserver.socket.response.admin.ProxyCommandResponse;
import pro.gravit.launchserver.socket.response.auth.*;
import pro.gravit.launchserver.socket.response.profile.BatchProfileByUsername;
import pro.gravit.launchserver.socket.response.profile.ProfileByUUIDResponse;
import pro.gravit.launchserver.socket.response.profile.ProfileByUsername;
import pro.gravit.launchserver.socket.response.secure.GetSecureTokenResponse;
import pro.gravit.launchserver.socket.response.secure.VerifySecureTokenResponse;
import pro.gravit.launchserver.socket.response.update.LauncherResponse;
import pro.gravit.launchserver.socket.response.update.UpdateListResponse;
import pro.gravit.launchserver.socket.response.update.UpdateResponse;
import pro.gravit.utils.ProviderMap;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

@SuppressWarnings("rawtypes")
public class WebSocketService {
    public final ChannelGroup channels;
    public static ProviderMap<WebSocketServerResponse> providers = new ProviderMap<>();

    public WebSocketService(ChannelGroup channels, LaunchServer server) {
        this.channels = channels;
        this.server = server;
        //this.gsonBuiler.registerTypeAdapter(WebSocketServerResponse.class, new JsonResponseAdapter(this));
        //this.gsonBuiler.registerTypeAdapter(WebSocketEvent.class, new JsonResultSerializeAdapter());
        //this.gsonBuiler.registerTypeAdapter(HashedEntry.class, new HashedEntryAdapter());
        this.gson = Launcher.gsonManager.gson;
    }

    private final LaunchServer server;
    private static final HashMap<String, Class> responses = new HashMap<>();
    private final Gson gson;

    @SuppressWarnings("unchecked")
	public void process(ChannelHandlerContext ctx, TextWebSocketFrame frame, Client client, String ip) {
        String request = frame.text();
        WebSocketServerResponse response = gson.fromJson(request, WebSocketServerResponse.class);
        if (server.config.netty.proxy.enabled) {
            if (server.config.netty.proxy.requests.contains(response.getType())) {

                UUID origRequestUUID = null;
                if (response instanceof SimpleResponse) {
                    SimpleResponse simpleResponse = (SimpleResponse) response;
                    simpleResponse.server = server;
                    simpleResponse.service = this;
                    simpleResponse.ctx = ctx;
                    if (ip != null) simpleResponse.ip = ip;
                    else simpleResponse.ip = IOHelper.getIP(ctx.channel().remoteAddress());
                    origRequestUUID = simpleResponse.requestUUID;
                }
                LogHelper.debug("Proxy %s request", response.getType());
                if (client.session == 0) client.session = new Random().nextLong();
                ProxyRequest proxyRequest = new ProxyRequest(response, client.session);
                if (response instanceof SimpleResponse) {
                    ((SimpleResponse) response).requestUUID = proxyRequest.requestUUID;
                }
                proxyRequest.isCheckSign = client.checkSign;
                try {
                    WebSocketEvent result = proxyRequest.request();
                    if (result instanceof AuthRequestEvent) {
                        LogHelper.debug("Client auth params get successful");
                        AuthRequestEvent authRequestEvent = (AuthRequestEvent) result;
                        client.isAuth = true;
                        client.session = authRequestEvent.session;
                        if (authRequestEvent.playerProfile != null)
                            client.username = authRequestEvent.playerProfile.username;
                    }
                    if (result instanceof Request && response instanceof SimpleResponse) {
                        ((Request) result).requestUUID = origRequestUUID;
                    }
                    sendObject(ctx, result);
                } catch (RequestException e) {
                    sendObject(ctx, new ErrorRequestEvent(e.getMessage()));
                } catch (Exception e) {
                    LogHelper.error(e);
                    RequestEvent event;
                    if (server.config.netty.sendExceptionEnabled) {
                        event = new ExceptionEvent(e);
                    } else {
                        event = new ErrorRequestEvent("Fatal server error. Contact administrator");
                    }
                    if (response instanceof SimpleResponse) {
                        event.requestUUID = ((SimpleResponse) response).requestUUID;
                    }
                    sendObject(ctx, event);
                }
                return;
            }
        }
        process(ctx, response, client, ip);
    }

    void process(ChannelHandlerContext ctx, WebSocketServerResponse response, Client client, String ip) {
        if (response instanceof SimpleResponse) {
            SimpleResponse simpleResponse = (SimpleResponse) response;
            simpleResponse.server = server;
            simpleResponse.service = this;
            simpleResponse.ctx = ctx;
            if (ip != null) simpleResponse.ip = ip;
            else simpleResponse.ip = IOHelper.getIP(ctx.channel().remoteAddress());
        }
        try {
            response.execute(ctx, client);
        } catch (Exception e) {
            LogHelper.error(e);
            RequestEvent event;
            if (server.config.netty.sendExceptionEnabled) {
                event = new ExceptionEvent(e);
            } else {
                event = new ErrorRequestEvent("Fatal server error. Contact administrator");
            }
            if (response instanceof SimpleResponse) {
                event.requestUUID = ((SimpleResponse) response).requestUUID;
            }
            sendObject(ctx, event);
        }
    }

    public Class getResponseClass(String type) {
        return responses.get(type);
    }

    public void registerClient(Channel channel) {
        channels.add(channel);
    }

    public static void registerResponses() {
        providers.register("auth", AuthResponse.class);
        providers.register("checkServer", CheckServerResponse.class);
        providers.register("joinServer", JoinServerResponse.class);
        providers.register("profiles", ProfilesResponse.class);
        providers.register("launcher", LauncherResponse.class);
        providers.register("updateList", UpdateListResponse.class);
        providers.register("cmdExec", ExecCommandResponse.class);
        providers.register("setProfile", SetProfileResponse.class);
        providers.register("addLogListener", AddLogListenerResponse.class);
        providers.register("update", UpdateResponse.class);
        providers.register("restoreSession", RestoreSessionResponse.class);
        providers.register("batchProfileByUsername", BatchProfileByUsername.class);
        providers.register("profileByUsername", ProfileByUsername.class);
        providers.register("profileByUUID", ProfileByUUIDResponse.class);
        providers.register("getSecureToken", GetSecureTokenResponse.class);
        providers.register("verifySecureToken", VerifySecureTokenResponse.class);
        providers.register("getAvailabilityAuth", GetAvailabilityAuthResponse.class);
        providers.register("proxy", ProxyCommandResponse.class);
        providers.register("register", RegisterResponse.class);
    }

    public void sendObject(ChannelHandlerContext ctx, Object obj) {
        ctx.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, WebSocketEvent.class)));
    }

    public void sendObject(ChannelHandlerContext ctx, Object obj, Type type) {
        ctx.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, type)));
    }

    public void sendObjectAll(Object obj) {
        for (Channel ch : channels) {
            ch.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, WebSocketEvent.class)));
        }
    }

    public void sendObjectAll(Object obj, Type type) {
        for (Channel ch : channels) {
            ch.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, type)));
        }
    }

    public void sendObjectAndClose(ChannelHandlerContext ctx, Object obj) {
        ctx.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, WebSocketEvent.class))).addListener(ChannelFutureListener.CLOSE);
    }

    public void sendObjectAndClose(ChannelHandlerContext ctx, Object obj, Type type) {
        ctx.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, type))).addListener(ChannelFutureListener.CLOSE);
    }

    public void sendEvent(EventResult obj) {
        channels.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj)));
    }

    public static class EventResult implements WebSocketEvent {
        public EventResult() {

        }

        @Override
        public String getType() {
            return "event";
        }
    }

}
