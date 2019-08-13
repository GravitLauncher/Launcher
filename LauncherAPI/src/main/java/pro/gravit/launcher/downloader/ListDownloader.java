package pro.gravit.launcher.downloader;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;

import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class ListDownloader {
    @FunctionalInterface
    public interface DownloadCallback {
        void stateChanged(String filename, long downloadedSize, long size);
    }

    @FunctionalInterface
    public interface DownloadTotalCallback {
        void addTotal(long size);
    }

    public static class DownloadTask {
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
            URI baseUri = new URI(base);
            String scheme = baseUri.getScheme();
            String host = baseUri.getHost();
            int port = baseUri.getPort();
            if (port != -1)
                host = host + ":" + port;
            String path = baseUri.getPath();
            for (DownloadTask apply : applies) {
                URI u = new URI(scheme, host, path + apply.apply, "", "");
                callback.stateChanged(apply.apply, 0L, apply.size);
                Path targetPath = dstDirFile.resolve(apply.apply);
                LogHelper.debug("Download URL: %s to file %s dir: %s", u.toString(), targetPath.toAbsolutePath().toString(), dstDirFile.toAbsolutePath().toString());
                if (get == null) get = new HttpGet(u);
                else {
                    get.reset();
                    get.setURI(u);
                }
                httpclient.execute(get, new FileDownloadResponseHandler(targetPath, apply, callback, totalCallback, false));
            }
        }
    }

    public void downloadZip(String base, List<DownloadTask> applies, Path dstDirFile, DownloadCallback callback, DownloadTotalCallback totalCallback, boolean fullDownload) throws IOException, URISyntaxException {
        /*try (CloseableHttpClient httpclient = HttpClients.custom()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build()) {
            HttpGet get;
            URI u = new URL(base).toURI();
            LogHelper.debug("Download ZIP URL: %s", u.toString());
            get = new HttpGet(u);
            httpclient.execute(get, new FileDownloadResponseHandler(dstDirFile, callback, totalCallback, true));
        }*/
        try (ZipInputStream input = IOHelper.newZipInput(new URL(base))) {
            for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                if (entry.isDirectory())
                    continue; // Skip directories
                // Unpack entry
                String name = entry.getName();
                LogHelper.subInfo("Downloading file: '%s'", name);
                if(fullDownload || applies.stream().anyMatch((t) -> t.apply.equals(name)))
                {
                    Path fileName = IOHelper.toPath(name);
                    transfer(input, dstDirFile.resolve(fileName), fileName.toString(), entry.getSize(), callback, totalCallback);
                }
            }
        }
    }

    public void downloadOne(String url, Path target) throws IOException, URISyntaxException {
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

    static class FileDownloadResponseHandler implements ResponseHandler<Path> {
        private final Path target;
        private final DownloadTask task;
        private final DownloadCallback callback;
        private final DownloadTotalCallback totalCallback;
        private final boolean zip;

        public FileDownloadResponseHandler(Path target) {
            this.target = target;
            this.task = null;
            this.zip = false;
            callback = null;
            totalCallback = null;
        }

        public FileDownloadResponseHandler(Path target, DownloadTask task, DownloadCallback callback, DownloadTotalCallback totalCallback, boolean zip) {
            this.target = target;
            this.task = task;
            this.callback = callback;
            this.totalCallback = totalCallback;
            this.zip = zip;
        }

        public FileDownloadResponseHandler(Path target, DownloadCallback callback, DownloadTotalCallback totalCallback, boolean zip) {
            this.target = target;
            this.task = null;
            this.callback = callback;
            this.totalCallback = totalCallback;
            this.zip = zip;
        }

        @Override
        public Path handleResponse(HttpResponse response) throws IOException {
            InputStream source = response.getEntity().getContent();
            int returnCode = response.getStatusLine().getStatusCode();
            if(returnCode != 200)
            {
                throw new IllegalStateException(String.format("Request download file %s return code %d", target.toString(), returnCode));
            }
            long contentLength = response.getEntity().getContentLength();
            if (task != null && contentLength != task.size)
            {
                LogHelper.warning("Missing content length: expected %d | found %d", task.size, contentLength);
            }
            if (zip) {
                try (ZipInputStream input = IOHelper.newZipInput(source)) {
                    ZipEntry entry = input.getNextEntry();
                    while (entry != null) {
                        if (entry.isDirectory()) {
                            entry = input.getNextEntry();
                            continue;
                        }
                        long size = entry.getSize();
                        String filename = entry.getName();
                        Path target = this.target.resolve(filename);
                        if (callback != null) {
                            callback.stateChanged(entry.getName(), 0, entry.getSize());
                        }
                        if (LogHelper.isDevEnabled()) {
                            LogHelper.dev("Resolved filename %s to %s", filename, target.toAbsolutePath().toString());
                        }
                        transfer(source, target, filename, size, callback, totalCallback);
                        entry = input.getNextEntry();
                    }

                }
                return null;
            }
            if (callback != null && task != null) {
                callback.stateChanged(task.apply, 0, task.size);
                transfer(source, this.target, task.apply, task.size, callback, totalCallback);
            } else
                IOHelper.transfer(source, this.target);
            return this.target;
        }
    }

    public static void transfer(InputStream input, Path file, String filename, long size, DownloadCallback callback, DownloadTotalCallback totalCallback) throws IOException {
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