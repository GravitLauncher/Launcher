package ru.gravit.launcher.downloader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

public class ListDownloader {
    @FunctionalInterface
    public interface DownloadCallback
    {
        void stateChanged(String filename,long downloadedSize, long size);
    }
    @FunctionalInterface
    public interface DownloadTotalCallback
    {
        void addTotal(long size);
    }
    public static class DownloadTask
    {
        public String apply;
        public long size;

        public DownloadTask(String apply, long size) {
            this.apply = apply;
            this.size = size;
        }
    }
    public void download(String base, List<DownloadTask> applies, Path dstDirFile, DownloadCallback callback, DownloadTotalCallback totalCallback) throws IOException, URISyntaxException {
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build()) {

            HttpGet get = null;
            for (DownloadTask apply : applies) {
                URI u = new URL(base.concat(escapeURL(apply.apply))).toURI();
                callback.stateChanged(apply.apply,0L, apply.size);
                LogHelper.debug("Download URL: %s", u.toString());
                if (get == null) get = new HttpGet(u);
                else {
                    get.reset();
                    get.setURI(u);
                }
                httpclient.execute(get, new FileDownloadResponseHandler(dstDirFile.resolve(apply.apply), apply, callback, totalCallback));
            }
        }
    }

    public void downloadOne(String url, Path target) throws IOException, URISyntaxException
    {
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build()) {

            HttpGet get;
            URI u = new URL(url).toURI();
            LogHelper.debug("Download URL: %s", u.toString());
            get = new HttpGet(u);
            httpclient.execute(get, new FileDownloadResponseHandler(target.toAbsolutePath()));
        }
    }

    public String escapeURL(String apply)
    {
        return apply.replaceAll(" ", "%20");
    }

    static class FileDownloadResponseHandler implements ResponseHandler<Path> {
        private final Path target;
        private final DownloadTask task;
        private final DownloadCallback callback;
        private final DownloadTotalCallback totalCallback;

        public FileDownloadResponseHandler(Path target) {
            this.target = target;
            this.task = null;
            callback = null;
            totalCallback = null;
        }

        public FileDownloadResponseHandler(Path target, DownloadTask task, DownloadCallback callback, DownloadTotalCallback totalCallback) {
            this.target = target;
            this.task = task;
            this.callback = callback;
            this.totalCallback = totalCallback;
        }

        @Override
        public Path handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            InputStream source = response.getEntity().getContent();
            if(callback != null && task != null)
            {
                callback.stateChanged(task.apply, 0, task.size);
                transfer(source, this.target, task.apply, task.size, callback, totalCallback);
            }
            else
                IOHelper.transfer(source, this.target);
            return this.target;
        }
    }
    public static void transfer(InputStream input, Path file, String filename, long size, DownloadCallback callback, DownloadTotalCallback totalCallback) throws IOException
    {
        try (OutputStream fileOutput = IOHelper.newOutput(file)) {
            long downloaded = 0L;

            // Download with digest update
            byte[] bytes = IOHelper.newBuffer();
            while (downloaded < size) {
                int remaining = (int) Math.min(size - downloaded, bytes.length);
                int length = input.read(bytes, 0, remaining);
                if (length < 0)
                    throw new EOFException(String.format("%d bytes remaining", size - downloaded));

                // Update file
                fileOutput.write(bytes, 0, length);

                // Update state
                downloaded += length;
                //totalDownloaded += length;
                totalCallback.addTotal(length);
                callback.stateChanged(filename, downloaded, size);
            }
        }
    }
}