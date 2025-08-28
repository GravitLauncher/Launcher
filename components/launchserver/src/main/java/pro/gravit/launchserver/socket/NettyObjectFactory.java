package pro.gravit.launchserver.socket;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;

public class NettyObjectFactory {
    private static NettyFactoryMode mode;

    public static void setMode(NettyFactoryMode mode) {
        NettyObjectFactory.mode = mode;
        if(mode == NettyFactoryMode.AUTO) {
            if(IoUring.isAvailable()) {
                NettyObjectFactory.mode = NettyFactoryMode.IO_URING;
                return;
            }
            if(Epoll.isAvailable()) {
                NettyObjectFactory.mode = NettyFactoryMode.EPOLL;
                return;
            }
            NettyObjectFactory.mode = NettyFactoryMode.NIO;
        }
    }

    public static NettyFactoryMode getMode() {
        return mode;
    }

    public static EventLoopGroup newEventLoopGroup(int threads, String poolName) {
        return switch (mode) {
            case AUTO -> null;
            case NIO -> new MultiThreadIoEventLoopGroup(threads, NioIoHandler.newFactory());
            case EPOLL -> new MultiThreadIoEventLoopGroup(threads, EpollIoHandler.newFactory());
            case IO_URING -> new MultiThreadIoEventLoopGroup(threads, IoUringIoHandler.newFactory());
        };
    }

    public static ChannelFactory<? extends ServerChannel> getServerSocketChannelFactory() {
        return switch (mode) {
            case AUTO -> null;
            case NIO -> NioServerSocketChannel::new;
            case EPOLL -> EpollServerSocketChannel::new;
            case IO_URING -> IoUringServerSocketChannel::new;
        };
    }

    public enum NettyFactoryMode {
        AUTO, NIO, EPOLL, IO_URING
    }
}
