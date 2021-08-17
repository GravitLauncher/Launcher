package pro.gravit.launchserver.socket.handlers;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;
import pro.gravit.launchserver.socket.NettyConnectContext;
import pro.gravit.utils.helper.IOHelper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class NettyWebAPIHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final TreeSet<SeverletPathPair> severletList = new TreeSet<>(Comparator.comparingInt((e) -> -e.key.length()));
    private static final DefaultFullHttpResponse ERROR_500 = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(IOHelper.encode("Internal Server Error 500")));

    static {
        ERROR_500.retain();
    }

    private final NettyConnectContext context;
    private transient final Logger logger = LogManager.getLogger();

    public NettyWebAPIHandler(NettyConnectContext context) {
        super();
        this.context = context;
    }

    public static SeverletPathPair addNewSeverlet(String path, SimpleSeverletHandler callback) {
        SeverletPathPair pair = new SeverletPathPair("/webapi/".concat(path), callback);
        severletList.add(pair);
        return pair;
    }

    public static SeverletPathPair addUnsafeSeverlet(String path, SimpleSeverletHandler callback) {
        SeverletPathPair pair = new SeverletPathPair(path, callback);
        severletList.add(pair);
        return pair;
    }

    public static void removeSeverlet(SeverletPathPair pair) {
        severletList.remove(pair);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        boolean isNext = true;
        for (SeverletPathPair pair : severletList) {
            if (msg.uri().startsWith(pair.key)) {
                try {
                    pair.callback.handle(ctx, msg, context);
                } catch (Throwable e) {
                    logger.error("WebAPI Error", e);
                    ctx.writeAndFlush(ERROR_500, ctx.voidPromise());
                }
                isNext = false;
                break;
            }
        }
        if (isNext) {
            msg.retain();
            ctx.fireChannelRead(msg);
        }
    }

    @FunctionalInterface
    public interface SimpleSeverletHandler {
        void handle(ChannelHandlerContext ctx, FullHttpRequest msg, NettyConnectContext context) throws Exception;

        default Map<String, String> getParamsFromUri(String uri) {
            int ind = uri.indexOf("?");
            if (ind <= 0) {
                return Map.of();
            }
            String sub = uri.substring(ind + 1);
            String[] result = sub.split("&");
            Map<String, String> map = new HashMap<>();
            for (String s : result) {
                String c = URLDecoder.decode(s, StandardCharsets.UTF_8);
                int index = c.indexOf("=");
                if (index <= 0) {
                    continue;
                }
                String key = c.substring(0, index);
                String value = c.substring(index + 1);
                map.put(key, value);
            }
            return map;
        }

        default FullHttpResponse simpleResponse(HttpResponseStatus status, String body) {
            return new DefaultFullHttpResponse(HTTP_1_1, status, body != null ? Unpooled.wrappedBuffer(IOHelper.encode(body)) : Unpooled.buffer());
        }

        default FullHttpResponse simpleJsonResponse(HttpResponseStatus status, Object body) {
            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, status, body != null ? Unpooled.wrappedBuffer(IOHelper.encode(Launcher.gsonManager.gson.toJson(body))) : Unpooled.buffer());
            httpResponse.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            return httpResponse;
        }

        default void sendHttpResponse(ChannelHandlerContext ctx, FullHttpResponse response) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public static class SeverletPathPair {
        public final String key;
        public final SimpleSeverletHandler callback;

        public SeverletPathPair(String key, SimpleSeverletHandler callback) {
            this.key = key;
            this.callback = callback;
        }
    }
}
