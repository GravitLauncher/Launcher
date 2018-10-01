package ru.gravit.launchserver.socket.websocket.json.update;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;
import ru.gravit.utils.Version;

public class LauncherResponse implements JsonResponseInterface {
    public Version version;
    public String hash;
    public int launcher_type;
    @Override
    public String getType() {
        return "launcherUpdate";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) throws Exception {
        if(launcher_type == 1) // JAR
        {
            if(hash.equals("1")) //REPLACE REAL HASH
            {
                service.sendObject(ctx, new Result(false));
            } else
            {
                service.sendObjectAndClose(ctx, new Result(true));
            }
        } else if(launcher_type == 2)
        {
            if(hash.equals("2")) //REPLACE REAL HASH
            {
                service.sendObject(ctx, new Result(false));
            } else
            {
                service.sendObjectAndClose(ctx, new Result(true));
            }
        }

    }
    public class Result
    {
        public String type = "success";
        public String requesttype = "launcherUpdate";

        public Result(boolean needUpdate) {
            this.needUpdate = needUpdate;
        }

        public boolean needUpdate;
    }
}
