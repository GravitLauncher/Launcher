package ru.gravit.utils.downloader;

import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Downloader implements Runnable {
    @FunctionalInterface
    public interface Handler {
        void check(Certificate[] certs) throws IOException;
    }

    public static final Map<String, String> requestClient = Collections.singletonMap("User-Agent",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
    public static final int INTERVAL = 300;

    private final File file;
    private final URL url;
    private final String method;
    public final Map<String, String> requestProps;
    public AtomicInteger writed = new AtomicInteger(0);
    public final AtomicBoolean interrupt = new AtomicBoolean(false);
    public final AtomicBoolean interrupted = new AtomicBoolean(false);
    public AtomicReference<Throwable> ex = new AtomicReference<>(null);
    private final int skip;
    private final Handler handler;

    private HttpURLConnection connect = null;

    public Downloader(URL url, File file) {
        this.requestProps = new HashMap<>(requestClient);
        this.file = file;
        this.url = url;
        this.skip = 0;
        this.handler = null;
        this.method = null;
    }

    public Downloader(URL url, File file, int skip) {
        this.requestProps = new HashMap<>(requestClient);
        this.file = file;
        this.url = url;
        this.skip = skip;
        this.handler = null;
        this.method = null;
    }

    public Downloader(URL url, File file, Handler handler) {
        this.requestProps = new HashMap<>(requestClient);
        this.file = file;
        this.url = url;
        this.skip = 0;
        this.handler = handler;
        this.method = null;
    }

    public Downloader(URL url, File file, int skip, Handler handler) {
        this.requestProps = new HashMap<>(requestClient);
        this.file = file;
        this.url = url;
        this.skip = skip;
        this.handler = handler;
        this.method = null;
    }

    public Downloader(URL url, File file, int skip, Handler handler, Map<String, String> requestProps) {
        this.requestProps = new HashMap<>(requestProps);
        this.file = file;
        this.url = url;
        this.skip = skip;
        this.handler = handler;
        this.method = null;
    }

    public Downloader(URL url, File file, int skip, Handler handler, Map<String, String> requestProps, String method) {
        this.requestProps = new HashMap<>(requestProps);
        this.file = file;
        this.url = url;
        this.skip = skip;
        this.handler = handler;
        this.method = method;
    }

    public Downloader(URL url, File file, int skip, Handler handler, String method) {
        this.requestProps = new HashMap<>(requestClient);
        this.file = file;
        this.url = url;
        this.skip = skip;
        this.handler = handler;
        this.method = method;
    }

    public Map<String, String> getProps() {
        return requestProps;
    }

    public void addProp(String key, String value) {
        requestProps.put(key, value);
    }

    public File getFile() {
        return file;
    }

    public String getMethod() {
        return method;
    }

    public Handler getHandler() {
        return handler;
    }

    public void downloadFile() throws IOException {
        if (!(url.getProtocol().equalsIgnoreCase("http") || url.getProtocol().equalsIgnoreCase("https")))
            throw new IOException("Invalid protocol.");
        interrupted.set(false);
        if (url.getProtocol().equalsIgnoreCase("http")) {
            HttpURLConnection connect = (HttpURLConnection) (url).openConnection();
            this.connect = connect;
            if (method != null) connect.setRequestMethod(method);
            for (Map.Entry<String, String> ent : requestProps.entrySet()) {
                connect.setRequestProperty(ent.getKey(), ent.getValue());
            }
            connect.setInstanceFollowRedirects(true);
            if (!(connect.getResponseCode() >= 200 && connect.getResponseCode() < 300))
                throw new IOException(String.format("Invalid response of http server %d.", connect.getResponseCode()));
            try (BufferedInputStream in = new BufferedInputStream(connect.getInputStream(), IOHelper.BUFFER_SIZE);
                 FileOutputStream fout = new FileOutputStream(file, skip != 0)) {
                byte data[] = new byte[IOHelper.BUFFER_SIZE];
                int count = -1;
                long timestamp = System.currentTimeMillis();
                int writed_local = 0;
                in.skip(skip);
                while ((count = in.read(data)) != -1) {
                    fout.write(data, 0, count);
                    writed_local += count;
                    if (System.currentTimeMillis() - timestamp > INTERVAL) {
                        writed.set(writed_local);
                        LogHelper.debug("Downloaded %d", writed_local);
                        if (interrupt.get()) {
                            break;
                        }
                    }
                }
                LogHelper.debug("Downloaded %d", writed_local);
                writed.set(writed_local);
            }
        } else {
            HttpsURLConnection connect = (HttpsURLConnection) (url).openConnection();
            this.connect = connect;
            if (method != null) connect.setRequestMethod(method);
            for (Map.Entry<String, String> ent : requestProps.entrySet()) {
                connect.setRequestProperty(ent.getKey(), ent.getValue());
            }
            connect.setInstanceFollowRedirects(true);
            if (handler != null)
                handler.check(connect.getServerCertificates());
            if (!(connect.getResponseCode() >= 200 && connect.getResponseCode() < 300))
                throw new IOException(String.format("Invalid response of http server %d.", connect.getResponseCode()));
            try (BufferedInputStream in = new BufferedInputStream(connect.getInputStream(), IOHelper.BUFFER_SIZE);
                 FileOutputStream fout = new FileOutputStream(file, skip != 0)) {
                byte data[] = new byte[IOHelper.BUFFER_SIZE];
                int count = -1;
                long timestamp = System.currentTimeMillis();
                int writed_local = 0;
                in.skip(skip);
                while ((count = in.read(data)) != -1) {
                    fout.write(data, 0, count);
                    writed_local += count;
                    if (System.currentTimeMillis() - timestamp > INTERVAL) {
                        writed.set(writed_local);
                        LogHelper.debug("Downloaded %d", writed_local);
                        if (interrupt.get()) {
                            break;
                        }
                    }
                }
                LogHelper.debug("Downloaded %d", writed_local);
                writed.set(writed_local);
            }
        }
        interrupted.set(true);
    }

    @Override
    public void run() {
        try {
            downloadFile();
        } catch (Throwable ex) {
            this.ex.set(ex);
            LogHelper.error(ex);
        }
        if (connect != null)
        	try {
        		connect.disconnect();
        	} catch (Throwable ignored) { }
    }
}
