package ru.gravit.launchserver.socket.websocket.json.update;

import java.util.Arrays;
import java.util.Base64;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;
import ru.gravit.utils.Version;

public class LauncherResponse implements JsonResponseInterface {
    public Version version;
    public String hash;
    public int launcher_type;
    //REPLACED TO REAL URL
    public static final String JAR_URL = "http://localhost:9752/Launcher.jar";
    public static final String EXE_URL = "http://localhost:9752/Launcher.exe";

    @Override
    public String getType() {
        return "launcherUpdate";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) {
        byte[] bytes = Base64.getDecoder().decode(hash);
        if (launcher_type == 1) // JAR
        {
            byte[] hash = LaunchServer.server.launcherBinary.getBytes().getDigest();
            if (hash == null) service.sendObjectAndClose(ctx, new Result(true, JAR_URL));
            if (Arrays.equals(bytes, hash)) {
                service.sendObject(ctx, new Result(false, JAR_URL));
            } else {
                service.sendObjectAndClose(ctx, new Result(true, JAR_URL));
            }
        } else if (launcher_type == 2) //EXE
        {
            byte[] hash = LaunchServer.server.launcherEXEBinary.getBytes().getDigest();
            if (hash == null) service.sendObjectAndClose(ctx, new Result(true, EXE_URL));
            if (Arrays.equals(bytes, hash)) {
                service.sendObject(ctx, new Result(false, EXE_URL));
            } else {
                service.sendObjectAndClose(ctx, new Result(true, EXE_URL));
            }
        } else service.sendObject(ctx, new WebSocketService.ErrorResult("Request launcher type error"));

    }

    public class Result {
        public String type = "success";
        public String requesttype = "launcherUpdate";
        public String url;

        public Result(boolean needUpdate, String url) {
            this.needUpdate = needUpdate;
            this.url = url;
        }

        public boolean needUpdate;
    }
}
