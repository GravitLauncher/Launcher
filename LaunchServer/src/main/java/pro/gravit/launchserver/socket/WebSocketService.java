package pro.gravit.launchserver.socket;

import com.google.gson.Gson;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatchers;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.events.ExceptionEvent;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.events.request.ErrorRequestEvent;
import pro.gravit.launcher.events.request.ExitRequestEvent;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.socket.handlers.WebSocketFrameHandler;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.launchserver.socket.response.WebSocketServerResponse;
import pro.gravit.launchserver.socket.response.auth.*;
import pro.gravit.launchserver.socket.response.management.FeaturesResponse;
import pro.gravit.launchserver.socket.response.management.PingServerReportResponse;
import pro.gravit.launchserver.socket.response.management.PingServerResponse;
import pro.gravit.launchserver.socket.response.management.ServerStatusResponse;
import pro.gravit.launchserver.socket.response.profile.BatchProfileByUsername;
import pro.gravit.launchserver.socket.response.profile.ProfileByUUIDResponse;
import pro.gravit.launchserver.socket.response.profile.ProfileByUsername;
import pro.gravit.launchserver.socket.response.secure.GetSecureLevelInfoResponse;
import pro.gravit.launchserver.socket.response.secure.HardwareReportResponse;
import pro.gravit.launchserver.socket.response.secure.SecurityReportResponse;
import pro.gravit.launchserver.socket.response.secure.VerifySecureLevelKeyResponse;
import pro.gravit.launchserver.socket.response.update.LauncherResponse;
import pro.gravit.launchserver.socket.response.update.UpdateListResponse;
import pro.gravit.launchserver.socket.response.update.UpdateResponse;
import pro.gravit.utils.BiHookSet;
import pro.gravit.utils.ProviderMap;
import pro.gravit.utils.helper.IOHelper;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WebSocketService {
    public static final ProviderMap<WebSocketServerResponse> providers = new ProviderMap<>();
    public final ChannelGroup channels;
    public final BiHookSet<WebSocketRequestContext, ChannelHandlerContext> hook = new BiHookSet<>();
    //Statistic data
    public final AtomicLong shortRequestLatency = new AtomicLong();
    public final AtomicLong shortRequestCounter = new AtomicLong();
    public final AtomicLong middleRequestLatency = new AtomicLong();
    public final AtomicLong middleRequestCounter = new AtomicLong();
    public final AtomicLong longRequestLatency = new AtomicLong();
    public final AtomicLong longRequestCounter = new AtomicLong();
    public final AtomicLong lastRequestTime = new AtomicLong();
    private final LaunchServer server;
    private final Gson gson;
    private transient final Logger logger = LogManager.getLogger();

    public WebSocketService(ChannelGroup channels, LaunchServer server) {
        this.channels = channels;
        this.server = server;
        this.gson = Launcher.gsonManager.gson;
    }

    public static void registerResponses() {
        providers.register("auth", AuthResponse.class);
        providers.register("checkServer", CheckServerResponse.class);
        providers.register("joinServer", JoinServerResponse.class);
        providers.register("profiles", ProfilesResponse.class);
        providers.register("launcher", LauncherResponse.class);
        providers.register("updateList", UpdateListResponse.class);
        providers.register("setProfile", SetProfileResponse.class);
        providers.register("update", UpdateResponse.class);
        providers.register("restoreSession", RestoreSessionResponse.class);
        providers.register("batchProfileByUsername", BatchProfileByUsername.class);
        providers.register("profileByUsername", ProfileByUsername.class);
        providers.register("profileByUUID", ProfileByUUIDResponse.class);
        providers.register("getAvailabilityAuth", GetAvailabilityAuthResponse.class);
        providers.register("exit", ExitResponse.class);
        providers.register("getSecureLevelInfo", GetSecureLevelInfoResponse.class);
        providers.register("verifySecureLevelKey", VerifySecureLevelKeyResponse.class);
        providers.register("securityReport", SecurityReportResponse.class);
        providers.register("hardwareReport", HardwareReportResponse.class);
        providers.register("serverStatus", ServerStatusResponse.class);
        providers.register("pingServerReport", PingServerReportResponse.class);
        providers.register("pingServer", PingServerResponse.class);
        providers.register("currentUser", CurrentUserResponse.class);
        providers.register("features", FeaturesResponse.class);
        providers.register("refreshToken", RefreshTokenResponse.class);
        providers.register("restore", RestoreResponse.class);
    }

    public void forEachActiveChannels(BiConsumer<Channel, WebSocketFrameHandler> callback) {
        for (Channel channel : channels) {
            if (channel == null || channel.pipeline() == null) continue;
            WebSocketFrameHandler wsHandler = channel.pipeline().get(WebSocketFrameHandler.class);
            if (wsHandler == null) continue;
            callback.accept(channel, wsHandler);
        }
    }

    public void process(ChannelHandlerContext ctx, TextWebSocketFrame frame, Client client, String ip) {
        long startTimeNanos = System.nanoTime();
        String request = frame.text();
        WebSocketServerResponse response = gson.fromJson(request, WebSocketServerResponse.class);
        if (response == null) {
            RequestEvent event = new ErrorRequestEvent("This type of request is not supported");
            sendObject(ctx, event);
            return;
        }
        process(ctx, response, client, ip);
        long executeTime = System.nanoTime() - startTimeNanos;
        if (executeTime > 0) {
            addRequestTimeToStats(executeTime);
        }
    }

    public void addRequestTimeToStats(long nanos) {
        if (nanos < 100_000_000L) // < 100 millis
        {
            shortRequestCounter.getAndIncrement();
            shortRequestLatency.getAndAdd(nanos);
        } else if (nanos < 1_000_000_000L) // > 100 millis and < 1 second
        {
            middleRequestCounter.getAndIncrement();
            middleRequestLatency.getAndAdd(nanos);
        } else // > 1 second
        {
            longRequestCounter.getAndIncrement();
            longRequestLatency.getAndAdd(nanos);
        }
        long lastTime = lastRequestTime.get();
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime > 60 * 1000) //1 minute
        {
            lastRequestTime.set(currentTime);
            shortRequestLatency.set(0);
            shortRequestCounter.set(0);
            middleRequestCounter.set(0);
            middleRequestLatency.set(0);
            longRequestCounter.set(0);
            longRequestLatency.set(0);
        }

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
            logger.error("WebSocket request processing failed", e);
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

    public void sendObjectToUUID(UUID userUuid, Object obj, Type type) {
        for (Channel ch : channels) {
            if (ch == null || ch.pipeline() == null) continue;
            WebSocketFrameHandler wsHandler = ch.pipeline().get(WebSocketFrameHandler.class);
            if (wsHandler == null) continue;
            Client client = wsHandler.getClient();
            if (client == null || !userUuid.equals(client.uuid)) continue;
            ch.writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, type)), ch.voidPromise());
        }
    }

    @Deprecated
    public void updateDaoObject(UUID userUuid, User daoObject, Consumer<Channel> callback) {
        for (Channel ch : channels) {
            if (ch == null || ch.pipeline() == null) continue;
            WebSocketFrameHandler wsHandler = ch.pipeline().get(WebSocketFrameHandler.class);
            if (wsHandler == null) continue;
            Client client = wsHandler.getClient();
            if (client == null || client.daoObject == null || !userUuid.equals(client.uuid)) continue;
            client.daoObject = daoObject;
            if (callback != null) callback.accept(ch);
        }
    }

    public Channel getChannelFromConnectUUID(UUID connectUuid) {
        for (Channel ch : channels) {
            if (ch == null || ch.pipeline() == null) continue;
            WebSocketFrameHandler wsHandler = ch.pipeline().get(WebSocketFrameHandler.class);
            if (wsHandler == null) continue;
            if (connectUuid.equals(wsHandler.getConnectUUID())) {
                return ch;
            }
        }
        return null;
    }

    public boolean kickByUserUUID(UUID userUuid, boolean isClose) {
        boolean result = false;
        for (Channel ch : channels) {
            if (ch == null || ch.pipeline() == null) continue;
            WebSocketFrameHandler wsHandler = ch.pipeline().get(WebSocketFrameHandler.class);
            if (wsHandler == null) continue;
            Client client = wsHandler.getClient();
            if (client == null || !userUuid.equals(client.uuid)) continue;
            ExitResponse.exit(server, wsHandler, ch, ExitRequestEvent.ExitReason.SERVER);
            if (isClose) ch.close();
            result = true;
        }
        return result;
    }

    public boolean kickByConnectUUID(UUID connectUuid, boolean isClose) {
        for (Channel ch : channels) {
            if (ch == null || ch.pipeline() == null) continue;
            WebSocketFrameHandler wsHandler = ch.pipeline().get(WebSocketFrameHandler.class);
            if (wsHandler == null) continue;
            if (connectUuid.equals(wsHandler.getConnectUUID())) {
                ExitResponse.exit(server, wsHandler, ch, ExitRequestEvent.ExitReason.SERVER);
                if (isClose) ch.close();
                return true;
            }
        }
        return false;
    }

    public boolean kickByIP(String ip, boolean isClose) {
        boolean result = false;
        for (Channel ch : channels) {
            if (ch == null || ch.pipeline() == null) continue;
            WebSocketFrameHandler wsHandler = ch.pipeline().get(WebSocketFrameHandler.class);
            if (wsHandler == null) continue;
            String clientIp;
            if (wsHandler.context != null && wsHandler.context.ip != null) clientIp = wsHandler.context.ip;
            else clientIp = IOHelper.getIP(ch.remoteAddress());
            if (ip.equals(clientIp)) {
                ExitResponse.exit(server, wsHandler, ch, ExitRequestEvent.ExitReason.SERVER);
                if (isClose) ch.close();
                result = true;
            }
        }
        return result;
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

    public static class EventResult implements WebSocketEvent {
        public EventResult() {

        }

        @Override
        public String getType() {
            return "event";
        }
    }

}
