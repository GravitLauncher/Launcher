package ru.gravit.launchserver.socket.websocket.json.update;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;
import ru.gravit.utils.Version;

import java.util.Arrays;
import java.util.Base64;

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
        byte[] bytes = Base64.getDecoder().decode(hash);
        if(launcher_type == 1) // JAR
        {
            byte[] hash = LaunchServer.server.launcherBinary.getHash();
            if(hash == null) service.sendObjectAndClose(ctx, new Result(true));
            if(Arrays.equals(bytes, hash)) //REPLACE REAL HASH
            {
                service.sendObject(ctx, new Result(false));
            } else
            {
                service.sendObjectAndClose(ctx, new Result(true));
            }
        } else if(launcher_type == 2) //EXE
        {
            byte[] hash = LaunchServer.server.launcherEXEBinary.getHash();
            if(hash == null) service.sendObjectAndClose(ctx, new Result(true));
            if(Arrays.equals(bytes, hash)) //REPLACE REAL HASH
            {
                service.sendObject(ctx, new Result(false));
            } else
            {
                service.sendObjectAndClose(ctx, new Result(true));
            }
        } else service.sendObject(ctx, new WebSocketService.ErrorResult("Request launcher type error"));

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
