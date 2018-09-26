package ru.gravit.launchserver.fileserver;

import java.io.File;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;

public class FileInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final File base;
	private final boolean outDirs;
    
    public FileInitializer(SslContext sslCtx, File base, boolean outDirs) {
        this.sslCtx = sslCtx;
        this.base = base;
        this.outDirs = outDirs;
    }

    public FileInitializer(File base, boolean outDirs) {
        this.sslCtx = null;
        this.base = base;
        this.outDirs = outDirs;
    }
	
    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new FileServerHandler(base, outDirs));
    }
}
