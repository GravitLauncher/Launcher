package pro.gravit.launchserver.hasher;

import java.io.IOError;
import java.io.IOException;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.hasher.HashedEntry;
import pro.gravit.utils.helper.IOHelper;

public final class UpdateData {
	private static final class Pair {
		private int cnt = 0;
		private long size = 0;
	}
	public static final int COUNT_LIMIT = 4;
	public static final long AVG_LIMIT = IOHelper.MB*4;
	public static final long TOTAL_LIMIT = IOHelper.MB*16;
	public static final double SIMPLE_DOWNLOAD_SIZE_COFF = 0.75D;
	public static final double SIMPLE_DOWNLOAD_FILE_COPF = 0.5D;

	private UpdateData() {
	}

	public static boolean needsZip(Pair p) {
        long avg = p.size/(long)p.cnt;
        return p.size < TOTAL_LIMIT && avg < AVG_LIMIT && p.cnt > COUNT_LIMIT;
	}

	public static boolean needsZip(HashedDir hDir) {
        return needsZip(count(hDir));
	}

	private static Pair count(HashedDir hDir) {
		final Pair pair = new Pair();
        try {
			hDir.walk(IOHelper.CROSS_SEPARATOR, (p,n,e) -> {
				if (e.getType().equals(HashedEntry.Type.FILE)) {
					pair.cnt++;
					pair.size += e.size();
				}
				return HashedDir.WalkAction.CONTINUE;
			});
		} catch (IOException e) {
			throw new IOError(e); // never happen
		}
        return pair;
	}
}