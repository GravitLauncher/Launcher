package pro.gravit.launchserver.socket;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyObjectFactory {
    private static boolean epoll = false;

    public static void setUsingEpoll(boolean value) {
        epoll = value;
    }

    public static EventLoopGroup newEventLoopGroup(int threads, String poolName) {
        if (epoll)
            return new EpollEventLoopGroup(threads);
        else
            return new NioEventLoopGroup(threads);
    }

    public static ChannelFactory<? extends ServerChannel> getServerSocketChannelFactory() {
        if (epoll)
            return EpollServerSocketChannel::new;
        else
            return NioServerSocketChannel::new;
    }
}
