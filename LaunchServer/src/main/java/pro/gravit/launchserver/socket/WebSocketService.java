package pro.gravit.launchserver.socket;

import com.google.gson.Gson;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.events.RequestEvent;
import pro.gravit.launcher.base.events.request.ErrorRequestEvent;
import pro.gravit.launcher.base.events.request.ExitRequestEvent;
import pro.gravit.launcher.base.request.WebSocketEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.handlers.WebSocketFrameHandler;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.launchserver.socket.response.WebSocketServerResponse;
import pro.gravit.launchserver.socket.response.auth.*;
import pro.gravit.launchserver.socket.response.cabinet.AssetUploadInfoResponse;
import pro.gravit.launchserver.socket.response.cabinet.GetAssetUploadInfoResponse;
import pro.gravit.launchserver.socket.response.management.FeaturesResponse;
import pro.gravit.launchserver.socket.response.management.GetConnectUUIDResponse;
import pro.gravit.launchserver.socket.response.management.GetPublicKeyResponse;
import pro.gravit.launchserver.socket.response.profile.BatchProfileByUsername;
import pro.gravit.launchserver.socket.response.profile.ProfileByUUIDResponse;
import pro.gravit.launchserver.socket.response.profile.ProfileByUsername;
import pro.gravit.launchserver.socket.response.secure.GetSecureLevelInfoResponse;
import pro.gravit.launchserver.socket.response.secure.HardwareReportResponse;
import pro.gravit.launchserver.socket.response.secure.SecurityReportResponse;
import pro.gravit.launchserver.socket.response.secure.VerifySecureLevelKeyResponse;
import pro.gravit.launchserver.socket.response.update.LauncherResponse;
import pro.gravit.launchserver.socket.response.update.UpdateResponse;
import pro.gravit.utils.BiHookSet;
import pro.gravit.utils.HookSet;
import pro.gravit.utils.ProviderMap;
import pro.gravit.utils.helper.IOHelper;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class WebSocketService {
    public static final ProviderMap<WebSocketServerResponse> providers = new ProviderMap<>();
    public final ChannelGroup channels;
    public final HookSet<WebSocketRequestContext> hookBeforeParsing = new HookSet<>();
    public final HookSet<WebSocketRequestContext> hookBeforeExecute = new HookSet<>();
    public final HookSet<WebSocketRequestContext> hookComplete = new HookSet<>();
    public final BiHookSet<Channel, Object> hookSend = new BiHookSet<>();
    private final LaunchServer server;
    private final Gson gson;
    private transient final Logger logger = LogManager.getLogger();
    private ExecutorService executors;

    public WebSocketService(ChannelGroup channels, LaunchServer server) {
        this.channels = channels;
        this.server = server;
        this.gson = Launcher.gsonManager.gson;
        executors = switch (server.config.netty.performance.executorType) {
            case NONE -> null;
            case DEFAULT -> Executors.newCachedThreadPool();
            case WORK_STEAL -> Executors.newWorkStealingPool();
            case VIRTUAL_THREADS -> Executors.newVirtualThreadPerTaskExecutor();
        };
    }

    public static void registerResponses() {
        providers.register("auth", AuthResponse.class);
        providers.register("checkServer", CheckServerResponse.class);
        providers.register("joinServer", JoinServerResponse.class);
        providers.register("profiles", ProfilesResponse.class);
        providers.register("launcher", LauncherResponse.class);
        providers.register("setProfile", SetProfileResponse.class);
        providers.register("update", UpdateResponse.class);
        providers.register("batchProfileByUsername", BatchProfileByUsername.class);
        providers.register("profileByUsername", ProfileByUsername.class);
        providers.register("profileByUUID", ProfileByUUIDResponse.class);
        providers.register("getAvailabilityAuth", GetAvailabilityAuthResponse.class);
        providers.register("exit", ExitResponse.class);
        providers.register("getSecureLevelInfo", GetSecureLevelInfoResponse.class);
        providers.register("verifySecureLevelKey", VerifySecureLevelKeyResponse.class);
        providers.register("securityReport", SecurityReportResponse.class);
        providers.register("hardwareReport", HardwareReportResponse.class);
        providers.register("currentUser", CurrentUserResponse.class);
        providers.register("features", FeaturesResponse.class);
        providers.register("refreshToken", RefreshTokenResponse.class);
        providers.register("restore", RestoreResponse.class);
        providers.register("additionalData", AdditionalDataResponse.class);
        providers.register("clientProfileKey", FetchClientProfileKeyResponse.class);
        providers.register("getPublicKey", GetPublicKeyResponse.class);
        providers.register("getAssetUploadUrl", GetAssetUploadInfoResponse.class);
        providers.register("assetUploadInfo", AssetUploadInfoResponse.class);
        providers.register("getConnectUUID", GetConnectUUIDResponse.class);
    }

    public static String getIPFromContext(ChannelHandlerContext ctx) {
        var handler = ctx.pipeline().get(WebSocketFrameHandler.class);
        if (handler == null || handler.context == null || handler.context.ip == null) {
            return IOHelper.getIP(ctx.channel().remoteAddress());
        }
        return handler.context.ip;
    }

    public static String getIPFromChannel(Channel channel) {
        var handler = channel.pipeline().get(WebSocketFrameHandler.class);
        if (handler == null || handler.context == null || handler.context.ip == null) {
            return IOHelper.getIP(channel.remoteAddress());
        }
        return handler.context.ip;
    }

    public void forEachActiveChannels(BiConsumer<Channel, WebSocketFrameHandler> callback) {
        for (Channel channel : channels) {
            if (channel == null || channel.pipeline() == null) continue;
            WebSocketFrameHandler wsHandler = channel.pipeline().get(WebSocketFrameHandler.class);
            if (wsHandler == null) continue;
            callback.accept(channel, wsHandler);
        }
    }

    public void process(ChannelHandlerContext ctx, TextWebSocketFrame frame, Client client, String ip, UUID connectUUID) {
        String request = frame.text();
        WebSocketRequestContext context = new WebSocketRequestContext(ctx, request, client, ip, connectUUID);
        if(hookBeforeParsing.hook(context)) {
            return;
        }
        WebSocketServerResponse response = gson.fromJson(request, WebSocketServerResponse.class);
        context.response = response;
        if (response == null) {
            RequestEvent event = new ErrorRequestEvent("This type of request is not supported");
            hookComplete.hook(context);
            sendObject(ctx.channel(), event, WebSocketEvent.class);
            return;
        }
        var safeStatus = server.config.netty.performance.disableThreadSafeClientObject ?
                WebSocketServerResponse.ThreadSafeStatus.NONE : response.getThreadSafeStatus();
        if(executors == null) {
            process(safeStatus, client, ip, context, response);
        } else {
            executors.submit(() -> process(safeStatus, client, ip, context, response));
        }
    }

    private void process(WebSocketServerResponse.ThreadSafeStatus safeStatus, Client client, String ip, WebSocketRequestContext context, WebSocketServerResponse response) {
        switch (safeStatus) {
            case NONE -> {
                process(context, response, client, ip);
            }
            case READ -> {
                var lock = client.lock.readLock();
                lock.lock();
                try {
                    process(context, response, client, ip);
                } finally {
                    lock.unlock();
                }
            }
            case READ_WRITE -> {
                var lock = client.lock.writeLock();
                lock.lock();
                try {
                    process(context, response, client, ip);
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    void process(WebSocketRequestContext context, WebSocketServerResponse response, Client client, String ip) {
        if (hookBeforeExecute.hook(context)) {
            return;
        }
        ChannelHandlerContext ctx = context.context;
        if (response instanceof SimpleResponse simpleResponse) {
            simpleResponse.server = server;
            simpleResponse.service = this;
            simpleResponse.ctx = ctx;
            if (ip != null) simpleResponse.ip = ip;
            else simpleResponse.ip = IOHelper.getIP(ctx.channel().remoteAddress());
            simpleResponse.connectUUID = context.connectUUID;
        }
        try {
            response.execute(ctx, client);
        } catch (Throwable e) {
            context.exception = e;
            logger.error("WebSocket request processing failed", e);
            RequestEvent event;
            event = new ErrorRequestEvent("Fatal server error. Contact administrator");
            if (response instanceof SimpleResponse simpleResponse) event.requestUUID = simpleResponse.requestUUID;
            sendObject(ctx.channel(), event);
        }
        hookComplete.hook(context);
    }

    public void registerClient(Channel channel) {
        channels.add(channel);
    }

    public void sendObject(Channel channel, Object obj) {
        if(hookSend.hook(channel, obj)) {
            return;
        }
        String msg = gson.toJson(obj, WebSocketEvent.class);
        if (logger.isTraceEnabled()) {
            logger.trace("Send to channel {}: {}", getIPFromChannel(channel), msg);
        }
        channel.writeAndFlush(new TextWebSocketFrame(msg), channel.voidPromise());
    }

    public void sendObject(Channel channel, Object obj, Type type) {
        if(hookSend.hook(channel, obj)) {
            return;
        }
        String msg = gson.toJson(obj, type);
        if (logger.isTraceEnabled()) {
            logger.trace("Send to channel {}: {}", getIPFromChannel(channel), msg);
        }
        channel.writeAndFlush(new TextWebSocketFrame(msg), channel.voidPromise());
    }

    public void sendObjectAll(Object obj, Type type) {
        for (Channel ch : channels) {
            sendObject(ch, obj, type);
        }
    }

    public void sendObjectToUUID(UUID userUuid, Object obj, Type type) {
        for (Channel ch : channels) {
            if (ch == null || ch.pipeline() == null) continue;
            WebSocketFrameHandler wsHandler = ch.pipeline().get(WebSocketFrameHandler.class);
            if (wsHandler == null) continue;
            Client client = wsHandler.getClient();
            if (client == null || !userUuid.equals(client.uuid)) continue;
            if(hookSend.hook(ch, obj)) {
                continue;
            }
            String msg = gson.toJson(obj, type);
            if (logger.isTraceEnabled()) {
                logger.trace("Send to {}({}): {}", getIPFromChannel(ch), userUuid, msg);
            }
            ch.writeAndFlush(new TextWebSocketFrame(msg), ch.voidPromise());
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
        if(hookSend.hook(ctx.channel(), obj)) {
            return;
        }
        String msg = gson.toJson(obj, WebSocketEvent.class);
        if (logger.isTraceEnabled()) {
            logger.trace("Send and close {}: {}", getIPFromContext(ctx), msg);
        }
        ctx.writeAndFlush(new TextWebSocketFrame(msg)).addListener(ChannelFutureListener.CLOSE);
    }

    public void sendObjectAndClose(ChannelHandlerContext ctx, Object obj, Type type) {
        if(hookSend.hook(ctx.channel(), obj)) {
            return;
        }
        String msg = gson.toJson(obj, type);
        if (logger.isTraceEnabled()) {
            logger.trace("Send and close {}: {}", getIPFromContext(ctx), msg);
        }
        ctx.writeAndFlush(new TextWebSocketFrame(msg)).addListener(ChannelFutureListener.CLOSE);
    }

    public static class WebSocketRequestContext {
        public final ChannelHandlerContext context;
        public final String text;
        public final Client client;
        public final String ip;
        public final UUID connectUUID;
        public WebSocketServerResponse response;
        public Throwable exception;

        public WebSocketRequestContext(ChannelHandlerContext context, String text, Client client, String ip, UUID connectUUID) {
            this.context = context;
            this.text = text;
            this.client = client;
            this.ip = ip;
            this.connectUUID = connectUUID;
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
