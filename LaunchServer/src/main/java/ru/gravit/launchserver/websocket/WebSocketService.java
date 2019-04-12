package ru.gravit.launchserver.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import ru.gravit.launcher.hasher.HashedEntry;
import ru.gravit.launcher.hasher.HashedEntryAdapter;
import ru.gravit.launcher.request.JsonResultSerializeAdapter;
import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.json.EchoResponse;
import ru.gravit.launchserver.websocket.json.JsonResponseAdapter;
import ru.gravit.launchserver.websocket.json.JsonResponseInterface;
import ru.gravit.launchserver.websocket.json.admin.AddLogListenerResponse;
import ru.gravit.launchserver.websocket.json.admin.ExecCommandResponse;
import ru.gravit.launchserver.websocket.json.auth.*;
import ru.gravit.launchserver.websocket.json.profile.BatchProfileByUsername;
import ru.gravit.launchserver.websocket.json.profile.ProfileByUUIDResponse;
import ru.gravit.launchserver.websocket.json.profile.ProfileByUsername;
import ru.gravit.launchserver.websocket.json.secure.GetSecureTokenResponse;
import ru.gravit.launchserver.websocket.json.secure.VerifySecureTokenResponse;
import ru.gravit.launchserver.websocket.json.update.LauncherResponse;
import ru.gravit.launchserver.websocket.json.update.UpdateListResponse;
import ru.gravit.launchserver.websocket.json.update.UpdateResponse;
import ru.gravit.utils.helper.LogHelper;

import java.lang.reflect.Type;
import java.util.HashMap;

@SuppressWarnings({"unused", "rawtypes"})
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

    void process(ChannelHandlerContext ctx, TextWebSocketFrame frame, Client client) {
        String request = frame.text();
        JsonResponseInterface response = gson.fromJson(request, JsonResponseInterface.class);
        try {
            response.execute(this, ctx, client);
        } catch (Exception e) {
            LogHelper.error(e);
            sendObject(ctx, new ExceptionResult(e));
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
        registerResponse("echo", EchoResponse.class);
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
    }

    public void sendObject(ChannelHandlerContext ctx, Object obj) {
        ctx.channel().writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, ResultInterface.class)));
    }

    public void sendObject(ChannelHandlerContext ctx, Object obj, Type type) {
        ctx.channel().writeAndFlush(new TextWebSocketFrame(gson.toJson(obj, type)));
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

    public static class ExceptionResult implements ResultInterface {
        public ExceptionResult(Exception e) {
            this.message = e.getMessage();
            this.clazz = e.getClass().getName();
            this.type = "exceptionError";
        }

        public final String message;
        public final String clazz;
        public final String type;

        @Override
        public String getType() {
            return "exception";
        }
    }
}
