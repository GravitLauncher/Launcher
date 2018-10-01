package ru.gravit.launchserver.socket.websocket.json;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.utils.helper.LogHelper;

public class EchoResponse implements JsonResponseInterface {
    public final String echo;

    public EchoResponse(String echo) {
        this.echo = echo;
    }

    @Override
    public String getType() {
        return "echo";
    }

    @Override
    public void execute(WebSocketService service,ChannelHandlerContext ctx) {
        LogHelper.info("Echo: %s",echo);
        service.sendObject(ctx,new Result(echo));
    }
    public class Result
    {
        String echo;

        public Result(String echo) {
            this.echo = echo;
        }
    }
}
