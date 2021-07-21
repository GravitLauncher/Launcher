package pro.gravit.launcher;

import pro.gravit.utils.helper.IOHelper;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public class AsyncDownloader {
    public static final Callback IGNORE = (ignored) -> {
    };
    @LauncherInject("launcher.certificatePinning")
    private static boolean isCertificatePinning;
    private static volatile SSLSocketFactory sslSocketFactory;
    private static volatile SSLContext sslContext;
    public final Callback callback;
    public volatile boolean isClosed;

    public AsyncDownloader(Callback callback) {
        this.callback = callback;
    }

    public AsyncDownloader() {
        callback = IGNORE;
    }

    public static SSLSocketFactory makeSSLSocketFactory() throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, KeyManagementException {
        if (sslSocketFactory != null) return sslSocketFactory;
        SSLContext sslContext = makeSSLContext();
        sslSocketFactory = sslContext.getSocketFactory();
        return sslSocketFactory;
    }

    public static SSLContext makeSSLContext() throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, KeyManagementException {
        if (sslContext != null) return sslContext;
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, CertificatePinningTrustManager.getTrustManager().getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    public void downloadFile(URL url, Path target, long size) throws IOException {
        if (isClosed) throw new IOException("Download interrupted");
        URLConnection connection = url.openConnection();
        if (isCertificatePinning) {
            HttpsURLConnection connection1 = (HttpsURLConnection) connection;
            try {
                connection1.setSSLSocketFactory(makeSSLSocketFactory());
            } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | KeyManagementException e) {
                throw new IOException(e);
            }
        }
        try (InputStream input = connection.getInputStream()) {
            transfer(input, target, size);
        }
    }

    public void downloadFile(URL url, Path target) throws IOException {
        URLConnection connection = url.openConnection();
        if (isCertificatePinning) {
            HttpsURLConnection connection1 = (HttpsURLConnection) connection;
            try {
                connection1.setSSLSocketFactory(makeSSLSocketFactory());
            } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | KeyManagementException e) {
                throw new IOException(e);
            }
        }
        try (InputStream input = connection.getInputStream()) {
            IOHelper.transfer(input, target);
        }
    }

    public void downloadListInOneThread(List<SizedFile> files, String baseURL, Path targetDir) throws URISyntaxException, IOException {
        URI baseUri = new URI(baseURL);
        String scheme = baseUri.getScheme();
        String host = baseUri.getHost();
        int port = baseUri.getPort();
        if (port != -1)
            host = host + ":" + port;
        String path = baseUri.getPath();
        for (AsyncDownloader.SizedFile currentFile : files) {
            URL url = new URI(scheme, host, path + currentFile.urlPath, "", "").toURL();
            downloadFile(url, targetDir.resolve(currentFile.filePath), currentFile.size);
        }
    }

    public void downloadListInOneThreadSimple(List<SizedFile> files, String baseURL, Path targetDir) throws URISyntaxException, IOException {

        for (AsyncDownloader.SizedFile currentFile : files) {
            downloadFile(new URL(baseURL + currentFile.urlPath), targetDir.resolve(currentFile.filePath), currentFile.size);
        }
    }

    public List<List<SizedFile>> sortFiles(List<SizedFile> files, int threads) {
        files.sort(Comparator.comparingLong((f) -> -f.size));
        List<List<SizedFile>> result = new ArrayList<>();
        for (int i = 0; i < threads; ++i) result.add(new LinkedList<>());
        long[] sizes = new long[threads];
        Arrays.fill(sizes, 0);
        for (SizedFile file : files) {
            long min = Long.MAX_VALUE;
            int minIndex = 0;
            for (int i = 0; i < threads; ++i)
                if (sizes[i] < min) {
                    min = sizes[i];
                    minIndex = i;
                }
            result.get(minIndex).add(file);
            sizes[minIndex] += file.size;
        }
        for (List<AsyncDownloader.SizedFile> list : result) {
            Collections.shuffle(list);
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    public CompletableFuture[] runDownloadList(List<List<SizedFile>> files, String baseURL, Path targetDir, Executor executor) {
        int threads = files.size();
        CompletableFuture[] futures = new CompletableFuture[threads];
        for (int i = 0; i < threads; ++i) {
            List<SizedFile> currentTasks = files.get(i);
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    downloadListInOneThread(currentTasks, baseURL, targetDir);
                } catch (URISyntaxException | IOException e) {
                    throw new CompletionException(e);
                }
            }, executor);
        }
        return futures;
    }

    @SuppressWarnings("rawtypes")
    public CompletableFuture[] runDownloadListSimple(List<List<SizedFile>> files, String baseURL, Path targetDir, Executor executor) {
        int threads = files.size();
        CompletableFuture[] futures = new CompletableFuture[threads];
        for (int i = 0; i < threads; ++i) {
            List<SizedFile> currentTasks = files.get(i);
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    downloadListInOneThreadSimple(currentTasks, baseURL, targetDir);
                } catch (URISyntaxException | IOException e) {
                    throw new CompletionException(e);
                }
            }, executor);
        }
        return futures;
    }

    public void transfer(InputStream input, Path file, long size) throws IOException {
        try (OutputStream fileOutput = IOHelper.newOutput(file)) {
            long downloaded = 0L;

            // Download with digest update
            byte[] bytes = IOHelper.newBuffer();
            while (downloaded < size) {
                if (isClosed) throw new IOException("Download interrupted");
                int remaining = (int) Math.min(size - downloaded, bytes.length);
                int length = input.read(bytes, 0, remaining);
                if (length < 0)
                    throw new EOFException(String.format("%d bytes remaining", size - downloaded));

                // Update file
                fileOutput.write(bytes, 0, length);

                // Update state
                downloaded += length;
                //totalDownloaded += length;
                callback.update(length);
            }
        }
    }

    @FunctionalInterface
    public interface Callback {
        void update(long diff);
    }

    public static class SizedFile {
        public final String urlPath, filePath;
        public final long size;

        public SizedFile(String path, long size) {
            this.urlPath = path;
            this.filePath = path;
            this.size = size;
        }

        public SizedFile(String urlPath, String filePath, long size) {
            this.urlPath = urlPath;
            this.filePath = filePath;
            this.size = size;
        }
    }
}
