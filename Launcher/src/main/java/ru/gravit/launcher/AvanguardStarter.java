package ru.gravit.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;

import ru.gravit.launcher.hasher.DirWatcher;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.SecurityHelper.DigestAlgorithm;
import ru.zaxar163.GuardBind;

public class AvanguardStarter {
	static class SecurityThread implements Runnable {
		@Override
		public void run() {
			while (!Thread.interrupted()) {
				try {
					if (!GuardBind.avnIsStarted()) {
						LogHelper.error("Avanguard stopped! Process stopped");
						System.exit(5);
					}
				} catch (NullPointerException e) {
					LogHelper.error("Avanguard unloaded! Process stopped");
					System.exit(6);
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					if (!GuardBind.avnIsStarted()) {
						LogHelper.error("Thread stopped! Process stopped");
						System.exit(7);
					}
				}
			}
		}
	}

	public static final String NAME = Launcher.getConfig().projectname;
	public static String avn32 = null, avn64 = null;
	public static Path wrap32 = null, wrap64 = null;

	private static Path handle(Path mustdiedll, String resource) {
		try {
			InputStream in = IOHelper.newInput(IOHelper.getResourceURL(resource));
			byte[] orig = IOHelper.toByteArray(in);
			in.close();
			if (IOHelper.exists(mustdiedll)) {
				if (!matches(mustdiedll, orig))
					transfer(orig, mustdiedll);
			} else
				transfer(orig, mustdiedll);
		} catch (Exception e) {
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException(e);
		}
		return mustdiedll;
	}

	public static void loadVared() {
		if (JVMHelper.JVM_BITS == 32)
			loadLib(System.getProperty("avn32"));
		else if (JVMHelper.JVM_BITS == 64)
			loadLib(System.getProperty("avn64"));
	}

	public static void start() {
       	loadVared();
		GuardBind.avnRegisterThreatNotifier((int threatType) -> {
			System.err.println("Threat " + GuardBind.ThreatType.getThreat(threatType).name());
			LogHelper.error("Cheating == crash!");
			System.exit(12);
			return false;
		});
		// нужно делать до пуска таймера!
		GuardBind.setCheckTime(3000);
		GuardBind.avnStartDefence();
		CommonHelper.newThread("Security Thread", true, new SecurityThread()).start();
	}

	private static boolean matches(Path mustdiedll, byte[] in) {
		try {
			return Arrays.equals(SecurityHelper.digest(DigestAlgorithm.MD5, in),
					SecurityHelper.digest(DigestAlgorithm.MD5, mustdiedll));
		} catch (IOException e) {
			return false;
		}
	}

	private static void processArched(Path arch32, Path arch64, Path wrapper32, Path wrapper64) {
		System.setProperty("avn32", IOHelper.toAbs(arch32));
		System.setProperty("avn64", IOHelper.toAbs(arch64));
		avn32 = IOHelper.toAbs(arch32);
		avn64 = IOHelper.toAbs(arch64);
		wrap32 = IOHelper.toAbsPath(wrapper32);
		wrap64 = IOHelper.toAbsPath(wrapper64);
	}

	public static void init(Path path1) throws IOException {
		Path path = path1.resolve("guard");
		processArched(handle(path.resolve("Avanguard32.dll"), "Avanguard32.dll"),
				handle(path.resolve("Avanguard64.dll"), "Avanguard64.dll"),
				handle(path.resolve(NAME + "32.exe"), "wrapper32.exe"),
				handle(path.resolve(NAME + "64.exe"), "wrapper64.exe"));
		HashedDir guard = new HashedDir(path, null, true, false);
		try (DirWatcher dirWatcher = new DirWatcher(path, guard, null, false)) {
			CommonHelper.newThread("Guard Directory Watcher", true, dirWatcher).start();
		}
	}

	private static void transfer(byte[] orig, Path mustdiedll) throws IOException {
		IOHelper.createParentDirs(mustdiedll);
		if (!IOHelper.exists(mustdiedll))
			mustdiedll.toFile().createNewFile();
		IOHelper.transfer(orig, mustdiedll, false);
	}

    public static void loadLib(String path) {
        LogHelper.debug("Anti-Cheat loading");
        System.load(path);
        LogHelper.debug("Anti-Cheat loaded");
    }
}
