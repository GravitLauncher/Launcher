package pro.gravit.launchserver.socket.handlers;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCounted;
import pro.gravit.launchserver.socket.NettyConnectContext;
import pro.gravit.utils.BiHookSet;
import pro.gravit.utils.HookSet;
import pro.gravit.utils.helper.LogHelper;

import java.net.URI;
import java.util.*;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

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
    private static TreeSet<SeverletPathPair> severletList = new TreeSet<>(Comparator.comparingInt((e) -> -e.key.length()));
    public static SeverletPathPair addNewSeverlet(String path, SimpleSeverletHandler callback)
    {
        SeverletPathPair pair = new SeverletPathPair("/webapi/".concat(path), callback);
        severletList.add(pair);
        return pair;
    }
    public static SeverletPathPair addUnsafeSeverlet(String path, SimpleSeverletHandler callback)
    {
        SeverletPathPair pair = new SeverletPathPair(path, callback);
        severletList.add(pair);
        return pair;
    }
    public static void removeSeverlet(SeverletPathPair pair)
    {
        severletList.remove(pair);
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        boolean isNext = true;
        for(SeverletPathPair pair : severletList)
        {
            if(msg.uri().startsWith(pair.key))
            {
                pair.callback.handle(ctx, msg, context);
                isNext = false;
                break;
            }
        }
        if(isNext)
        {
            msg.retain();
            ctx.fireChannelRead(msg);
        }
    }
}
