package pro.gravit.launcher.downloader;

import java.io.IOError;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.hasher.HashedEntry;
import pro.gravit.utils.helper.IOHelper;

public final class UpdateData {
	public static final int COUNT_LIMIT = 4;
	public static final long AVG_LIMIT = IOHelper.MB*4;
	public static final long TOTAL_LIMIT = IOHelper.MB*16;
	
	private UpdateData() {
	}

	public static boolean needsZip(HashedDir hDir) {
        AtomicLong size = new AtomicLong();
        AtomicInteger cnt = new AtomicInteger();
        try {
			hDir.walk(IOHelper.CROSS_SEPARATOR, (p,n,e) -> {
				if (e.getType().equals(HashedEntry.Type.FILE)) {
					cnt.incrementAndGet();
					size.addAndGet(e.size());
				}
				return HashedDir.WalkAction.CONTINUE;
			});
		} catch (IOException e) {
			throw new IOError(e); // never happen
		}
        long avg = size.get()/(long)cnt.get();
        return size.get() < TOTAL_LIMIT && avg < AVG_LIMIT && cnt.get() > COUNT_LIMIT;
	}
}
