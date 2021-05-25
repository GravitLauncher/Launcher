package pro.gravit.launchserver.socket.handlers.fileserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import pro.gravit.launchserver.socket.handlers.ContentType;
import pro.gravit.utils.helper.VerifyHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class FileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static final DateTimeFormatter dateFormatter;
    public static final String READ = "r";
    public static final int HTTP_CACHE_SECONDS = VerifyHelper.verifyInt(Integer.parseInt(System.getProperty("launcher.fileserver.cachesec", "60")), VerifyHelper.NOT_NEGATIVE, "HttpCache seconds should be positive");
    private static final boolean OLD_ALGO = Boolean.parseBoolean(System.getProperty("launcher.fileserver.oldalgo", "true"));
    private static final ContentType TYPE_PROBE = Arrays.stream(ContentType.values()).filter(e -> e.name().toLowerCase(Locale.US).equals(System.getProperty("launcher.fileserver.typeprobe", "nio"))).findFirst().orElse(ContentType.UNIVERSAL);
    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[^-\\._]?[^<>&\\\"]*");

    static {
        dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).withZone(ZoneId.of("UTC"));
    }

    private final Path base;
    private final boolean fullOut;
    private final boolean showHiddenFiles;

    public FileServerHandler(Path base, boolean fullOut, boolean showHiddenFiles) {
        this.base = base;
        this.fullOut = fullOut;
        this.showHiddenFiles = showHiddenFiles;
    }

    private static void sendListing(ChannelHandlerContext ctx, File dir, String dirPath, boolean showHidden) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

        StringBuilder buf = new StringBuilder()
                .append("<!DOCTYPE html>\r\n")
                .append("<html><head><meta charset='utf-8' /><title>")
                .append("Listing of: ")
                .append(dirPath)
                .append("</title></head><body>\r\n")

                .append("<h3>Listing of: ")
                .append(dirPath)
                .append("</h3>\r\n")

                .append("<ul>")
                .append("<li><a href=\"../\">..</a></li>\r\n");

        for (File f : Objects.requireNonNull(dir.listFiles())) {
            if ((f.isHidden() && !showHidden) || !f.canRead()) {
                continue;
            }

            String name = f.getName();
            if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
                continue;
            }

            buf.append("<li><a href=\"")
                    .append(name)
                    .append("\">")
                    .append(name)
                    .append("</a></li>\r\n");
        }

        buf.append("</ul></body></html>\r\n");
        ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void sendRedirect(ChannelHandlerContext ctx, String newUri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
        response.headers().set(HttpHeaderNames.LOCATION, newUri);

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * When file timestamp is the same as what the browser is sending up, send a "304 Not Modified"
     *
     * @param ctx Context
     */
    private static void sendNotModified(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED);
        setDateHeader(response);

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Sets the Date header for the HTTP response
     *
     * @param response HTTP response
     */
    private static void setDateHeader(FullHttpResponse response) {
        response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(Instant.ofEpochMilli(System.currentTimeMillis())));
    }

    /**
     * Sets the Date and Cache headers for the HTTP Response
     *
     * @param response    HTTP response
     * @param fileToCache file to extract content type
     */
    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        // Date header
        LocalDateTime time = LocalDateTime.now(Clock.systemUTC());
        response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time));

        // Add cache headers
        response.headers().set(HttpHeaderNames.EXPIRES, dateFormatter.format(time.plus(HTTP_CACHE_SECONDS, ChronoUnit.SECONDS)));
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(
                HttpHeaderNames.LAST_MODIFIED, dateFormatter.format(Instant.ofEpochMilli(fileToCache.lastModified())));
    }

    /**
     * Sets the content type header for the HTTP Response
     *
     * @param response HTTP response
     * @param file     file to extract content type
     */
    private static void setContentTypeHeader(HttpResponse response, File file) {
        String contentType = TYPE_PROBE.forPath(file);
        if (contentType != null)
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        if (request.method() != GET) {
            sendError(ctx, METHOD_NOT_ALLOWED);
            return;
        }

        final String uri = request.uri();
        final String path;

        try {
            path = Paths.get(new URI(uri).getPath()).normalize().toString().substring(1);
        } catch (URISyntaxException e) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        File file = base.resolve(path).toFile();
        if ((file.isHidden() && !showHiddenFiles) || !file.exists()) {
            sendError(ctx, NOT_FOUND);
            return;
        }

        if (file.isDirectory()) {
            if (fullOut) {
                if (uri.endsWith("/")) {
                    sendListing(ctx, file, uri, showHiddenFiles);
                } else {
                    sendRedirect(ctx, uri + '/');
                }
            } else sendError(ctx, NOT_FOUND); // can not handle dirs
            return;
        }

        if (!file.isFile()) {
            sendError(ctx, FORBIDDEN);
            return;
        }

        // Cache Validation
        String ifModifiedSince = request.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            TemporalAccessor ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

            // Only compare up to the second because the datetime format we send to the client
            // does not have milliseconds
            try {
                long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getLong(ChronoField.INSTANT_SECONDS);
                long fileLastModifiedSeconds = file.lastModified() / 1000;
                if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                    sendNotModified(ctx);
                    return;
                }
            } catch (UnsupportedTemporalTypeException ignored) {
            }
        }

        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, READ);
        } catch (FileNotFoundException ignore) {
            sendError(ctx, NOT_FOUND);
            return;
        }
        long fileLength = raf.length();

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        HttpUtil.setContentLength(response, fileLength);
        setContentTypeHeader(response, file);
        setDateAndCacheHeaders(response, file);
        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        // Write the initial line and the header.
        ctx.write(response);

        // Write the content.
        ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;
        if (OLD_ALGO) {
            sendFileFuture =
                    ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
            // Write the end marker.
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            lastContentFuture.addListener(new ClosingChannelFutureListener(raf));
        } else {
            sendFileFuture =
                    ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)),
                            ctx.newProgressivePromise());
            // HttpChunkedInput will write the end marker (LastHttpContent) for us.
            lastContentFuture = sendFileFuture;
        }

        // Decide whether to close the connection or not.
        if (!HttpUtil.isKeepAlive(request)) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }
}
