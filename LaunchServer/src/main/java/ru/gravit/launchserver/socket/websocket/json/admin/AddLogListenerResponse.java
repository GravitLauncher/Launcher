package ru.gravit.launchserver.socket.websocket.json.admin;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.ErrorRequestEvent;
import ru.gravit.launcher.events.request.LogEvent;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;
import ru.gravit.utils.helper.LogHelper;

public class AddLogListenerResponse implements JsonResponseInterface {
    public LogHelper.OutputTypes outputType = LogHelper.OutputTypes.PLAIN;
    @Override
    public String getType() {
        return "addLogListener";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) throws Exception {
        if (!client.isAuth) {
            service.sendObject(ctx, new ErrorRequestEvent("Access denied"));
            return;
        }
        if (!client.permissions.canAdmin) {
            service.sendObject(ctx, new ErrorRequestEvent("Access denied"));
            return;
        }
        if(client.logOutput != null)
        {
            LogHelper.info("Client %s remove log listener", client.username);
            LogHelper.removeOutput(client.logOutput);
        }
        else
        {
            LogHelper.info("Client %s add log listener", client.username);
            LogHelper.Output output = (str) -> {
                if(!ctx.isRemoved())
                {
                    service.sendObject(ctx,new LogEvent(str));
                }
                else {
                    LogHelper.removeOutput(client.logOutput);
                    LogHelper.info("Client %s remove log listener", client.username);
                }
            };
            client.logOutput = new LogHelper.OutputEnity(output,outputType);
            LogHelper.addOutput(client.logOutput);
        }
    }
}
