package pro.gravit.launchserver.socket.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import pro.gravit.launchserver.socket.NettyConnectContext;

import java.util.Comparator;
import java.util.TreeSet;

public class NettyWebAPIHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final NettyConnectContext context;

    public NettyWebAPIHandler(NettyConnectContext context) {
        super();
        this.context = context;
    }

    @FunctionalInterface
    public interface SimpleSeverletHandler {
        void handle(ChannelHandlerContext ctx, FullHttpRequest msg, NettyConnectContext context) throws Exception;
    }

    public static class SeverletPathPair {
        public final String key;
        public final SimpleSeverletHandler callback;

        public SeverletPathPair(String key, SimpleSeverletHandler callback) {
            this.key = key;
            this.callback = callback;
        }
    }

    private static final TreeSet<SeverletPathPair> severletList = new TreeSet<>(Comparator.comparingInt((e) -> -e.key.length()));

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
                pair.callback.handle(ctx, msg, context);
                isNext = false;
                break;
            }
        }
        if (isNext) {
            msg.retain();
            ctx.fireChannelRead(msg);
        }
    }
}
