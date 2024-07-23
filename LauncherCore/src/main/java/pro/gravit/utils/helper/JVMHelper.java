package pro.gravit.utils.helper;

import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Locale;

public final class JVMHelper {

    // MXBeans exports
    public static final RuntimeMXBean RUNTIME_MXBEAN = ManagementFactory.getRuntimeMXBean();
    public static final OperatingSystemMXBean OPERATING_SYSTEM_MXBEAN =
            ManagementFactory.getOperatingSystemMXBean();
    public static final OS OS_TYPE = OS.byName(OPERATING_SYSTEM_MXBEAN.getName());
    public static final int OS_BITS = getCorrectOSArch();
    // System properties
    public static final String OS_VERSION = OPERATING_SYSTEM_MXBEAN.getVersion();
    public static final ARCH ARCH_TYPE = getArch(System.getProperty("os.arch"));
    public static final String NATIVE_EXTENSION = getNativeExtension(OS_TYPE);
    public static final String NATIVE_PREFIX = getNativePrefix(OS_TYPE);
    public static final int JVM_BITS = Integer.parseInt(System.getProperty("sun.arch.data.model"));
    // Public static fields
    public static final Runtime RUNTIME = Runtime.getRuntime();
    public static final ClassLoader LOADER = ClassLoader.getSystemClassLoader();
    public static final int JVM_VERSION = getVersion();
    public static final int JVM_BUILD = getBuild();

    static {
        try {
            MethodHandles.publicLookup(); // Just to initialize class
        } catch (Throwable exc) {
            throw new InternalError(exc);
        }
    }

    private JVMHelper() {
    }

    public static ARCH getArch(String arch) {
        if (arch.equals("amd64") || arch.equals("x86-64") || arch.equals("x86_64")) return ARCH.X86_64;
        if (arch.equals("i386") || arch.equals("i586") || arch.equals("i686") || arch.equals("x86")) return ARCH.X86;
        if (arch.startsWith("armv8") || arch.startsWith("aarch64")) return ARCH.ARM64;
        if (arch.startsWith("arm") || arch.startsWith("aarch32")) return ARCH.ARM32;
        throw new InternalError(String.format("Unsupported arch '%s'", arch));
    }

    public static int getVersion() {
        //System.out.println("[DEBUG] JVMHelper 11 version");
        return Runtime.version().feature();
    }

    public static int getBuild() {
        return Runtime.version().update();
    }

    public static String getNativeExtension(JVMHelper.OS OS_TYPE) {
        return switch (OS_TYPE) {
            case MUSTDIE -> ".dll";
            case LINUX -> ".so";
            case MACOSX -> ".dylib";
        };
    }

    public static String getNativePrefix(JVMHelper.OS OS_TYPE) {
        return switch (OS_TYPE) {
            case LINUX, MACOSX -> "lib";
            default -> "";
        };
    }

    public static void fullGC() {
        RUNTIME.gc();
        LogHelper.debug("Used heap: %d MiB", RUNTIME.totalMemory() - RUNTIME.freeMemory() >> 20);
    }

    public static X509Certificate[] getCertificates(Class<?> clazz) {
        Object[] signers = clazz.getSigners();
        if (signers == null) return null;
        return Arrays.stream(signers).filter((c) -> c instanceof X509Certificate).map((c) -> (X509Certificate) c).toArray(X509Certificate[]::new);
    }

    public static void checkStackTrace(Class<?> mainClass) {
        LogHelper.debug("Testing stacktrace");
        Exception e = new Exception("Testing stacktrace");
        StackTraceElement[] list = e.getStackTrace();
        if (!list[list.length - 1].getClassName().equals(mainClass.getName())) {
            throw new SecurityException(String.format("Invalid StackTraceElement: %s", list[list.length - 1].getClassName()));
        }
    }

    private static int getCorrectOSArch() {
        // As always, mustdie must die
        if (OS_TYPE == OS.MUSTDIE)
            return System.getenv("ProgramFiles(x86)") == null ? 32 : 64;

        // Or trust system property (maybe incorrect)
        return System.getProperty("os.arch").contains("64") ? 64 : 32;
    }

    public static boolean isJVMMatchesSystemArch() {
        return JVM_BITS == OS_BITS;
    }

    public static String jvmProperty(String name, String value) {
        return String.format("-D%s=%s", name, value);
    }

    public static void verifySystemProperties(Class<?> mainClass, boolean requireSystem) {
        Locale.setDefault(Locale.US);
        // Verify class loader
        LogHelper.debug("Verifying class loader");
        if (requireSystem && !mainClass.getClassLoader().equals(LOADER))
            throw new SecurityException("ClassLoader should be system");

        // Verify system and java architecture
        LogHelper.debug("Verifying JVM architecture");
    }

    public enum ARCH {
        X86("x86"), X86_64("x86-64"), ARM64("arm64"), ARM32("arm32");

        public final String name;

        ARCH(String name) {
            this.name = name;
        }
    }

    public enum OS {
        MUSTDIE("mustdie"), LINUX("linux"), MACOSX("macosx");

        public final String name;

        OS(String name) {
            this.name = name;
        }

        public static OS byName(String name) {
            if (name.startsWith("Windows"))
                return MUSTDIE;
            if (name.startsWith("Linux"))
                return LINUX;
            if (name.startsWith("Mac OS X"))
                return MACOSX;
            throw new RuntimeException(String.format("This shit is not yet supported: '%s'", name));
        }
    }

}
