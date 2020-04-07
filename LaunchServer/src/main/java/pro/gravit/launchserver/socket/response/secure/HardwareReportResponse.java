package pro.gravit.launchserver.socket.response.secure;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class HardwareReportResponse extends SimpleResponse {
    public String pathToJava;
    public String javaVersion;

    @Override
    public String getType() {
        return "hardwareReport";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {

    }
}
