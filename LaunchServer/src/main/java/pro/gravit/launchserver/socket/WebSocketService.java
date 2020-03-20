package pro.gravit.launchserver.socket;

import com.google.gson.Gson;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatchers;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.events.ExceptionEvent;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.events.request.ErrorRequestEvent;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.launchserver.socket.response.WebSocketServerResponse;
import pro.gravit.launchserver.socket.response.admin.AddLogListenerResponse;
import pro.gravit.launchserver.socket.response.admin.ExecCommandResponse;
import pro.gravit.launchserver.socket.response.auth.*;
import pro.gravit.launchserver.socket.response.profile.BatchProfileByUsername;
import pro.gravit.launchserver.socket.response.profile.ProfileByUUIDResponse;
import pro.gravit.launchserver.socket.response.profile.ProfileByUsername;
import pro.gravit.launchserver.socket.response.secure.GetSecureLevelInfoResponse;
import pro.gravit.launchserver.socket.response.secure.VerifySecureLevelKeyResponse;
import pro.gravit.launchserver.socket.response.update.LauncherResponse;
import pro.gravit.launchserver.socket.response.update.UpdateListResponse;
import pro.gravit.launchserver.socket.response.update.UpdateResponse;
import pro.gravit.utils.BiHookSet;
import pro.gravit.utils.ProviderMap;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.lang.reflect.Type;

public class WebSocketService {
    public final ChannelGroup channels;
    public static final ProviderMap<WebSocketServerResponse> providers = new ProviderMap<>();

    public static class WebSocketRequestContext {
        public final WebSocketServerResponse response;
        public final Client client;
        public final String ip;

        public WebSocketRequestContext(WebSocketServerResponse response, Client client, String ip) {
            this.response = response;
            this.client = client;
            this.ip = ip;
        }
    }

    public final BiHookSet<WebSocketRequestContext, ChannelHandlerContext> hook = new BiHookSet<>();

    public WebSocketService(ChannelGroup channels, LaunchServer server) {
        this.channels = channels;
        this.server = server;
        //this.gsonBuiler.registerTypeAdapter(WebSocketServerResponse.class, new JsonResponseAdapter(this));
        //this.gsonBuiler.registerTypeAdapter(WebSocketEvent.class, new JsonResultSerializeAdapter());
        //this.gsonBuiler.registerTypeAdapter(HashedEntry.class, new HashedEntryAdapter());
        this.gson = Launcher.gsonManager.gson;
    }

    private final LaunchServer server;
    private final Gson gson;

    public void process(ChannelHandlerContext ctx, TextWebSocketFrame frame, Client client, String ip) {
        String request = frame.text();
        WebSocketServerResponse response = gson.fromJson(request, WebSocketServerResponse.class);
        if(response == null)
        {
            RequestEvent event= new ErrorRequestEvent("This type of request is not supported");
            sendObject(ctx, event);
        }
        process(ctx, response, client, ip);
    }

    void process(ChannelHandlerContext ctx, WebSocketServerResponse response, Client client, String ip) {
        WebSocketRequestContext context = new WebSocketRequestContext(response, client, ip);
        if (hook.hook(context, ctx)) {
            return;
        }
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
        providers.register("getAvailabilityAuth", GetAvailabilityAuthResponse.class);
        providers.register("register", RegisterResponse.class);
        providers.register("setPassword", SetPasswordResponse.class);
        providers.register("exit", ExitResponse.class);
        providers.register("getSecureLevelInfo", GetSecureLevelInfoResponse.class);
        providers.register("verifySecureLevelKey", VerifySecureLevelKeyResponse.class);
    }

    public void sendObject(ChannelHandlerContext ctx, Object obj) {
        ctx.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, WebSocketEvent.class)), ctx.voidPromise());
    }

    public void sendObject(ChannelHandlerContext ctx, Object obj, Type type) {
        ctx.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, type)), ctx.voidPromise());
    }

    public void sendObject(Channel channel, Object obj) {
        channel.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, WebSocketEvent.class)), channel.voidPromise());
    }

    public void sendObject(Channel channel, Object obj, Type type) {
        channel.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, type)), channel.voidPromise());
    }

    public void sendObjectAll(Object obj) {
        for (Channel ch : channels) {
            ch.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, WebSocketEvent.class)), ch.voidPromise());
        }
    }

    public void sendObjectAll(Object obj, Type type) {
        for (Channel ch : channels) {
            ch.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, type)), ch.voidPromise());
        }
    }

    public void sendObjectAndClose(ChannelHandlerContext ctx, Object obj) {
        ctx.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, WebSocketEvent.class))).addListener(ChannelFutureListener.CLOSE);
    }

    public void sendObjectAndClose(ChannelHandlerContext ctx, Object obj, Type type) {
        ctx.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, type))).addListener(ChannelFutureListener.CLOSE);
    }

    public void sendEvent(EventResult obj) {
        channels.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj)), ChannelMatchers.all(), true);
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
