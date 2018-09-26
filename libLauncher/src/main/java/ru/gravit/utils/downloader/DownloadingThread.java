package ru.gravit.utils.downloader;

import java.io.File;
import java.net.URL;

public class DownloadingThread extends Thread {
	private final Downloader runnable;
	
	public DownloadingThread(File file, URL url, String name) {
		super(name);
		runnable = new Downloader(url, file);
	}
	
	public Downloader getDownloader() {
		return runnable;
	}
	
	@Override
	public void interrupt() {
		runnable.interrupt.set(true);
		while (!runnable.interrupted.get()) {
			;
		}
		super.interrupt();
	}
	
	public void hardInterrupt() {
		super.interrupt();
	}
	
	@Override
	public void run() {
		runnable.run();
	}
}
