package pro.gravit.launchserver.socket;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyObjectFactory {
    private static boolean epoll = false;
    public static void setUsingEpoll(boolean value)
    {
        epoll = value;
    }
    public static EventLoopGroup newEventLoopGroup(int threads)
    {
        if(epoll)
            return new EpollEventLoopGroup(threads);
        else
            return new NioEventLoopGroup(threads);
    }
    public static Class<? extends ServerChannel> getServerSocketChannelClass()
    {
        if(epoll)
            return EpollServerSocketChannel.class;
        else
            return NioServerSocketChannel.class;
    }

}
