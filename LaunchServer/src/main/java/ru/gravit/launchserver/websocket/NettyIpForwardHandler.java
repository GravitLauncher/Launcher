package ru.gravit.launchserver.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.ReferenceCounted;
import ru.gravit.utils.helper.LogHelper;

import java.util.List;

public class NettyIpForwardHandler extends MessageToMessageDecoder<HttpRequest> {
    private NettyConnectContext context;

    public NettyIpForwardHandler(NettyConnectContext context) {
        super();
        this.context = context;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpRequest msg, List<Object> out) throws Exception {
        if (msg instanceof ReferenceCounted) {
            ((ReferenceCounted) msg).retain();
        }
        if (context.ip != null) {
            out.add(msg);
            return;
        }
        HttpHeaders headers = msg.headers();
        String realIP = null;
        if (headers.contains("X-Forwarded-For")) {
            realIP = headers.get("X-Forwarded-For");
        }
        if (headers.contains("X-Real-IP")) {
            realIP = headers.get("X-Real-IP");
        }
        if (realIP != null) {
            LogHelper.dev("Real IP address %s", realIP);
            context.ip = realIP;
        } else LogHelper.error("IpForwarding error. Headers not found");
        out.add(msg);
    }
}
