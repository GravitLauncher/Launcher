package ru.gravit.launchserver.socket.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.websocket.json.JsonResponse;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseAdapter;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.InputStreamReader;
import java.io.Reader;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    public static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public static Gson gson;
    public WebSocketService service = new WebSocketService(LaunchServer.server,gson);

    public static LaunchServer server;
    public static GsonBuilder builder = new GsonBuilder();
    static {
        builder.registerTypeAdapter(JsonResponse.class,new JsonResponseAdapter());
        gson = builder.create();
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LogHelper.debug("New client %s", IOHelper.getIP(ctx.channel().remoteAddress()));
        channels.add(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        // ping and pong frames already handled
        if (frame instanceof TextWebSocketFrame) {
            service.process(ctx, (TextWebSocketFrame) frame);
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }
}
