package pro.gravit.launchserver.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import pro.gravit.launchserver.websocket.json.JsonResponseAdapter;
import pro.gravit.launchserver.websocket.json.JsonResponseInterface;
import pro.gravit.launchserver.websocket.json.SimpleResponse;
import pro.gravit.launchserver.websocket.json.auth.*;
import pro.gravit.launcher.events.ExceptionEvent;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.events.request.ErrorRequestEvent;
import pro.gravit.launcher.hasher.HashedEntry;
import pro.gravit.launcher.hasher.HashedEntryAdapter;
import pro.gravit.launcher.request.JsonResultSerializeAdapter;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.ResultInterface;
import pro.gravit.launcher.request.admin.ProxyRequest;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.websocket.json.admin.AddLogListenerResponse;
import pro.gravit.launchserver.websocket.json.admin.ExecCommandResponse;
import pro.gravit.launchserver.websocket.json.admin.ProxyCommandResponse;
import pro.gravit.launchserver.websocket.json.auth.*;
import pro.gravit.launchserver.websocket.json.profile.BatchProfileByUsername;
import pro.gravit.launchserver.websocket.json.profile.ProfileByUUIDResponse;
import pro.gravit.launchserver.websocket.json.profile.ProfileByUsername;
import pro.gravit.launchserver.websocket.json.secure.GetSecureTokenResponse;
import pro.gravit.launchserver.websocket.json.secure.VerifySecureTokenResponse;
import pro.gravit.launchserver.websocket.json.update.LauncherResponse;
import pro.gravit.launchserver.websocket.json.update.UpdateListResponse;
import pro.gravit.launchserver.websocket.json.update.UpdateResponse;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

@SuppressWarnings("rawtypes")
public class WebSocketService {
    public final ChannelGroup channels;

    public WebSocketService(ChannelGroup channels, LaunchServer server, GsonBuilder gson) {
        this.channels = channels;
        this.server = server;
        this.gsonBuiler = gson;
        this.gsonBuiler.registerTypeAdapter(JsonResponseInterface.class, new JsonResponseAdapter(this));
        this.gsonBuiler.registerTypeAdapter(ResultInterface.class, new JsonResultSerializeAdapter());
        this.gsonBuiler.registerTypeAdapter(HashedEntry.class, new HashedEntryAdapter());
        this.gson = gsonBuiler.create();
    }

    private final LaunchServer server;
    private static final HashMap<String, Class> responses = new HashMap<>();
    private final Gson gson;
    private final GsonBuilder gsonBuiler;

    @SuppressWarnings("unchecked")
    void process(ChannelHandlerContext ctx, TextWebSocketFrame frame, Client client, String ip) {
        String request = frame.text();
        JsonResponseInterface response = gson.fromJson(request, JsonResponseInterface.class);
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
                    ResultInterface result = proxyRequest.request();
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

    void process(ChannelHandlerContext ctx, JsonResponseInterface response, Client client, String ip) {
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

    public void registerResponse(String key, Class responseInterfaceClass) {
        responses.put(key, responseInterfaceClass);
    }

    public void registerClient(Channel channel) {
        channels.add(channel);
    }

    public void registerResponses() {
        registerResponse("auth", AuthResponse.class);
        registerResponse("checkServer", CheckServerResponse.class);
        registerResponse("joinServer", JoinServerResponse.class);
        registerResponse("profiles", ProfilesResponse.class);
        registerResponse("launcher", LauncherResponse.class);
        registerResponse("updateList", UpdateListResponse.class);
        registerResponse("cmdExec", ExecCommandResponse.class);
        registerResponse("setProfile", SetProfileResponse.class);
        registerResponse("addLogListener", AddLogListenerResponse.class);
        registerResponse("update", UpdateResponse.class);
        registerResponse("restoreSession", RestoreSessionResponse.class);
        registerResponse("batchProfileByUsername", BatchProfileByUsername.class);
        registerResponse("profileByUsername", ProfileByUsername.class);
        registerResponse("profileByUUID", ProfileByUUIDResponse.class);
        registerResponse("getSecureToken", GetSecureTokenResponse.class);
        registerResponse("verifySecureToken", VerifySecureTokenResponse.class);
        registerResponse("getAvailabilityAuth", GetAvailabilityAuthResponse.class);
        registerResponse("proxy", ProxyCommandResponse.class);
    }

    public void sendObject(ChannelHandlerContext ctx, Object obj) {
        ctx.channel().writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, ResultInterface.class)));
    }

    public void sendObject(ChannelHandlerContext ctx, Object obj, Type type) {
        ctx.channel().writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, type)));
    }

    public void sendObjectAll(Object obj) {
        for (Channel ch : channels) {
            ch.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, ResultInterface.class)));
        }
    }

    public void sendObjectAll(Object obj, Type type) {
        for (Channel ch : channels) {
            ch.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, type)));
        }
    }

    public void sendObjectAndClose(ChannelHandlerContext ctx, Object obj) {
        ctx.channel().writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, ResultInterface.class))).addListener(ChannelFutureListener.CLOSE);
    }

    public void sendObjectAndClose(ChannelHandlerContext ctx, Object obj, Type type) {
        ctx.channel().writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, type))).addListener(ChannelFutureListener.CLOSE);
    }

    public void sendEvent(EventResult obj) {
        channels.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj)));
    }

    public static class EventResult implements ResultInterface {
        public EventResult() {

        }

        @Override
        public String getType() {
            return "event";
        }
    }

}
