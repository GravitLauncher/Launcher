package ru.gravit.launchserver.websocket.json.admin;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.LogEvent;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.json.SimpleResponse;
import ru.gravit.utils.helper.LogHelper;

public class AddLogListenerResponse extends SimpleResponse {
    public LogHelper.OutputTypes outputType = LogHelper.OutputTypes.PLAIN;

    @Override
    public String getType() {
        return "addLogListener";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        if (!client.isAuth) {
            sendError("Access denied");
            return;
        }
        if (!client.permissions.canAdmin) {
            sendError("Access denied");
            return;
        }
        if (client.logOutput != null) {
            LogHelper.info("Client %s remove log listener", client.username);
            LogHelper.removeOutput(client.logOutput);
        } else {
            LogHelper.info("Client %s add log listener", client.username);
            LogHelper.Output output = (str) -> {
                if (!ctx.isRemoved()) {
                    service.sendObject(ctx, new LogEvent(str));
                } else {
                    LogHelper.removeOutput(client.logOutput);
                    LogHelper.info("Client %s remove log listener", client.username);
                }
            };
            client.logOutput = new LogHelper.OutputEnity(output, outputType);
            LogHelper.addOutput(client.logOutput);
        }
    }
}
