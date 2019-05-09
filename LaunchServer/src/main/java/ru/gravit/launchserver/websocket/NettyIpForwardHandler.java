package ru.gravit.launchserver.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import ru.gravit.utils.helper.LogHelper;

public class NettyIpForwardHandler extends ChannelInboundHandlerAdapter {
    private NettyConnectContext context;

    public NettyIpForwardHandler(NettyConnectContext context) {
        super();
        this.context = context;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //super.channelRead(ctx, msg);
        if(context.ip != null)
        {
            ctx.writeAndFlush(msg);
            return;
        }
        if(msg instanceof HttpRequest)
        {
            HttpRequest http = (HttpRequest) msg;
            HttpHeaders headers = http.headers();
            String realIP = null;
            if(headers.contains("X-Forwarded-For"))
            {
                realIP = headers.get("X-Forwarded-For");
            }
            if(headers.contains("X-Real-IP"))
            {
                realIP = headers.get("X-Real-IP");
            }
            if(realIP != null) {
                LogHelper.dev("Real IP address %s", realIP);
                context.ip = realIP;
            }
            else LogHelper.error("IpForwarding error. Headers not found");
        }
        else LogHelper.error("IpForwarding error. Real message class %s", msg.getClass().getName());
        ctx.writeAndFlush(msg);
    }
}
