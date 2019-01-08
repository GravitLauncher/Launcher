package ru.gravit.utils;

import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class HttpDownloader extends Observable {
    public static final int BUFER_SIZE = 8192;
    public static final int INTERVAL = 300;
    public AtomicInteger writed = new AtomicInteger(0);
    private String filename;
    public Thread thread;

    public HttpDownloader(URL url, String file) {
        Runnable run = () -> {
            try {
                filename = file;
                downloadFile(url, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        Thread downloader = new Thread(run);
        thread = downloader;
        downloader.start();
    }

    public synchronized String getFilename() {
        return filename;
    }

    public void downloadFile(URL url, String file) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(url.openStream()); FileOutputStream fout = new FileOutputStream(file)) {

            final byte data[] = new byte[BUFER_SIZE];
            int count;
            long timestamp = System.currentTimeMillis();
            int writed_local = 0;
            while ((count = in.read(data, 0, BUFER_SIZE)) != -1) {
                fout.write(data, 0, count);
                writed_local += count;
                if (System.currentTimeMillis() - timestamp > INTERVAL) {
                    writed.set(writed_local);
                    LogHelper.debug("Downloaded %d", writed_local);
                }
            }
            writed.set(writed_local);
        }
    }

    public static void downloadZip(URL url, Path dir) throws IOException {
        try (ZipInputStream input = IOHelper.newZipInput(url)) {
            for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                if (entry.isDirectory())
                    continue; // Skip directories
                // Unpack entry
                String name = entry.getName();
                LogHelper.subInfo("Downloading file: '%s'", name);
                IOHelper.transfer(input, dir.resolve(IOHelper.toPath(name)));
            }
        }
    }
}
