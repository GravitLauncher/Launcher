package ru.gravit.utils.downloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import ru.gravit.utils.helper.LogHelper;

public class Downloader implements Runnable {
    public static final int BUFER_SIZE = 8192;
    public static final int INTERVAL = 300;
    public AtomicInteger writed = new AtomicInteger(0);
    private final File file;
    private final URL url;
    public final AtomicBoolean interrupt = new AtomicBoolean(false);
	private final int skip;
	public AtomicReference<IOException> ex = new AtomicReference<>(null);
	
	public Downloader(URL url, File file) {
		this.file = file;
        this.url = url;
        this.skip = 0;
    }

	public Downloader(URL url, File file, int skip) {
		this.file = file;
        this.url = url;
        this.skip = skip;
    }
	
    public File getFile() {
        return file;
    }

    public void downloadFile() throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(url.openStream()); FileOutputStream fout = new FileOutputStream(file, skip != 0)) {
            final byte data[] = new byte[BUFER_SIZE];
            int count;
            long timestamp = System.currentTimeMillis();
            int writed_local = 0;
            in.skip(skip);
            while ((count = in.read(data, 0, BUFER_SIZE)) != -1) {
                fout.write(data, 0, count);
                writed_local += count;
                if (System.currentTimeMillis() - timestamp > INTERVAL) {
                    writed.set(writed_local);
                    LogHelper.debug("Downloaded %d", writed_local);
                    if (interrupt.get()) break;
                }
            }
            writed.set(writed_local);
        }
    }

	@Override
	public void run() {
		try {
			downloadFile();
		} catch (IOException ex) {
			this.ex.set(ex);
			LogHelper.error(ex);
		}		
	}
}