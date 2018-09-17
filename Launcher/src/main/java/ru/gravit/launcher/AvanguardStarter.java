package ru.gravit.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;

import ru.gravit.launcher.helper.CommonHelper;
import ru.gravit.launcher.helper.IOHelper;
import ru.gravit.launcher.helper.JVMHelper;
import ru.gravit.launcher.helper.LogHelper;
import ru.gravit.launcher.helper.SecurityHelper;
import ru.gravit.launcher.helper.SecurityHelper.DigestAlgorithm;
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
	public static final String NAME = "wrapper"; // TODO customizable filename.
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
            GuardBind.startAbs(System.getProperty("avn32"));
        else if (JVMHelper.JVM_BITS == 64)
            GuardBind.startAbs(System.getProperty("avn64"));
    }

    public static void main(boolean init) {
        if (init)
            GuardBind.init();
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

    public static void start(Path path1) {
        Path path = path1.resolve("guard");
        processArched(handle(path.resolve(NAME + "32.dll"), "Avanguard32.dll"),
                handle(path.resolve(NAME + "64.dll"), "Avanguard64.dll"),
                handle(path.resolve(NAME + "32.exe"), "wrapper32.exe"),
                handle(path.resolve(NAME + "64.exe"), "wrapper64.exe"));
    }

    private static void transfer(byte[] orig, Path mustdiedll) throws IOException {
        IOHelper.createParentDirs(mustdiedll);
        if (!IOHelper.exists(mustdiedll))
            mustdiedll.toFile().createNewFile();
        IOHelper.transfer(orig, mustdiedll, false);
    }
}
