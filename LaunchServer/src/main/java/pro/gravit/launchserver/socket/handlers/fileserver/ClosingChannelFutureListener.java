package pro.gravit.launchserver.socket.handlers.fileserver;

import java.io.Closeable;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import pro.gravit.utils.helper.IOHelper;

public class ClosingChannelFutureListener implements ChannelFutureListener {
    public final Closeable[] close;

    public ClosingChannelFutureListener(Closeable... close) {
        this.close = close;
    }


    @Override
    public void operationComplete(ChannelFuture future) {
        for (Closeable cl : close) IOHelper.close(cl);
    }

}
