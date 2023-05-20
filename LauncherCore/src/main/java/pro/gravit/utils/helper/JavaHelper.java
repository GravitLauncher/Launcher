package pro.gravit.utils.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class JavaHelper {
    private static List<JavaVersion> javaVersionsCache;

    public static Path tryGetOpenJFXPath(Path jvmDir) {
        String dirName = jvmDir.getFileName().toString();
        Path parent = jvmDir.getParent();
        if (parent == null) return null;
        Path archJFXPath = parent.resolve(dirName.replace("openjdk", "openjfx"));
        if (Files.isDirectory(archJFXPath)) {
            return archJFXPath;
        }
        Path arch2JFXPath = parent.resolve(dirName.replace("jdk", "openjfx"));
        if (Files.isDirectory(arch2JFXPath)) {
            return arch2JFXPath;
        }
        if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
            Path debianJfxPath = Paths.get("/usr/share/openjfx");
            if (Files.isDirectory(debianJfxPath)) {
                return debianJfxPath;
            }
        }
        return null;
    }

    public static Path tryFindModule(Path path, String moduleName) {
        Path result = path.resolve(moduleName.concat(".jar"));
        if (!IOHelper.isFile(result))
            result = path.resolve("lib").resolve(moduleName.concat(".jar"));
        else return result;
        if (!IOHelper.isFile(result))
            return null;
        else return result;
    }

    public static boolean tryAddModule(List<Path> paths, String moduleName, StringBuilder args) {
        for (Path path : paths) {
            if (path == null) continue;
            Path result = tryFindModule(path, moduleName);
            if (result != null) {
                if (args.length() != 0) args.append(File.pathSeparatorChar);
                args.append(result.toAbsolutePath());
                return true;
            }
        }
        return false;
    }

    public synchronized static List<JavaVersion> findJava() {
        if (javaVersionsCache != null) {
            return javaVersionsCache;
        }
        List<String> javaPaths = new ArrayList<>(4);
        List<JavaVersion> result = new ArrayList<>(4);
        tryAddJava(javaPaths, result, JavaVersion.getCurrentJavaVersion());
        String[] path = System.getenv("PATH").split(JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE ? ";" : ":");
        for (String p : path) {
            try {
                Path p1 = Paths.get(p);
                Path javaExecPath = JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE ? p1.resolve("java.exe") : p1.resolve("java");
                if (Files.exists(javaExecPath)) {
                    javaExecPath = javaExecPath.toRealPath();
                    p1 = javaExecPath.getParent().getParent();
                    if(p1 == null) {
                        continue;
                    }
                    tryAddJava(javaPaths, result, JavaVersion.getByPath(p1));
                    trySearchJava(javaPaths, result, p1.getParent());
                }
            } catch (InvalidPathException | NullPointerException ignored) {

            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) {
            Path rootDrive = IOHelper.getRoot();
            try {
                trySearchJava(javaPaths, result, rootDrive.resolve("Program Files").resolve("Java"));
                trySearchJava(javaPaths, result, rootDrive.resolve("Program Files").resolve("AdoptOpenJDK"));
                trySearchJava(javaPaths, result, rootDrive.resolve("Program Files").resolve("Eclipse Foundation")); //AdoptJDK rebranding
                trySearchJava(javaPaths, result, rootDrive.resolve("Program Files").resolve("BellSoft")); // LibericaJDK
            } catch (IOException e) {
                LogHelper.error(e);
            }
        } else if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
            try {
                trySearchJava(javaPaths, result, Paths.get("/usr/lib/jvm"));
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        javaVersionsCache = result;
        return result;
    }

    private static JavaVersion tryFindJavaByPath(Path path) {
        if (javaVersionsCache == null) {
            return null;
        }
        for (JavaVersion version : javaVersionsCache) {
            if (version.jvmDir.equals(path)) {
                return version;
            }
        }
        return null;
    }

    public static void tryAddJava(List<String> javaPaths, List<JavaVersion> result, JavaVersion version) {
        if (version == null) return;
        Path realPath = version.jvmDir.toAbsolutePath();
        try {
            realPath = realPath.toRealPath();
        } catch (IOException ignored) {

        }
        String path = realPath.toString();
        if (javaPaths.contains(path)) return;
        javaPaths.add(path);
        result.add(version);
    }

    public static void trySearchJava(List<String> javaPaths, List<JavaVersion> result, Path path) throws IOException {
        if (path == null || !Files.isDirectory(path)) return;
        Files.list(path).filter(p -> Files.exists(p.resolve("bin").resolve(JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE ? "java.exe" : "java"))).forEach(e -> {
            tryAddJava(javaPaths, result, JavaVersion.getByPath(e));
            if (Files.exists(e.resolve("jre"))) {
                tryAddJava(javaPaths, result, JavaVersion.getByPath(e.resolve("jre")));
            }
        });
    }

    public static JavaVersionAndBuild getJavaVersion(String version) {
        JavaVersionAndBuild result = new JavaVersionAndBuild();
        if (version.startsWith("1.")) {
            result.version = Integer.parseInt(version.substring(2, 3));
            int pos = version.indexOf('_');
            if (pos != -1) {
                result.build = Integer.parseInt(version.substring(pos + 1));
            }
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                result.version = Integer.parseInt(version.substring(0, dot));
                dot = version.lastIndexOf(".");
                result.build = Integer.parseInt(version.substring(dot + 1));
            } else {
                try {
                    if (version.endsWith("-ea")) {
                        version = version.substring(0, version.length() - 3);
                    }
                    result.version = Integer.parseInt(version);
                    result.build = 0;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }

    public static class JavaVersionAndBuild {
        public int version;
        public int build;

        public JavaVersionAndBuild(int version, int build) {
            this.version = version;
            this.build = build;
        }

        public JavaVersionAndBuild() {
        }
    }

    public static class JavaVersion {
        public final Path jvmDir;
        public final int version;
        public final int build;
        public final JVMHelper.ARCH arch;
        public boolean enabledJavaFX;

        public JavaVersion(Path jvmDir, int version) {
            this.jvmDir = jvmDir;
            this.version = version;
            this.build = 0;
            this.arch = JVMHelper.ARCH_TYPE;
            this.enabledJavaFX = true;
        }

        public JavaVersion(Path jvmDir, int version, int build, boolean enabledJavaFX) {
            this.jvmDir = jvmDir;
            this.version = version;
            this.build = build;
            this.arch = JVMHelper.ARCH_TYPE;
            this.enabledJavaFX = enabledJavaFX;
        }

        public JavaVersion(Path jvmDir, int version, int build, JVMHelper.ARCH arch, boolean enabledJavaFX) {
            this.jvmDir = jvmDir;
            this.version = version;
            this.build = build;
            this.arch = arch;
            this.enabledJavaFX = enabledJavaFX;
        }

        public static JavaVersion getCurrentJavaVersion() {
            return new JavaVersion(Paths.get(System.getProperty("java.home")), JVMHelper.getVersion(), JVMHelper.JVM_BUILD, JVMHelper.ARCH_TYPE, isCurrentJavaSupportJavaFX());
        }

        private static boolean isCurrentJavaSupportJavaFX() {
            try {
                Class.forName("javafx.application.Application");
                return true;
            } catch (ClassNotFoundException e) {
                if (JVMHelper.getVersion() > 8) {
                    Path jvmDir = Paths.get(System.getProperty("java.home"));
                    return tryFindModule(jvmDir, "javafx.base") != null;
                }
                return false;
            }
        }

        public static JavaVersion getByPath(Path jvmDir) {
            {
                JavaVersion version = JavaHelper.tryFindJavaByPath(jvmDir);
                if (version != null) {
                    return version;
                }
            }
            Path releaseFile = jvmDir.resolve("release");
            JavaVersionAndBuild versionAndBuild = null;
            JVMHelper.ARCH arch = JVMHelper.ARCH_TYPE;
            if (IOHelper.isFile(releaseFile)) {
                try {
                    Properties properties = new Properties();
                    properties.load(IOHelper.newReader(releaseFile));
                    versionAndBuild = getJavaVersion(properties.getProperty("JAVA_VERSION").replaceAll("\"", ""));
                    try {
                        arch = JVMHelper.getArch(properties.getProperty("OS_ARCH").replaceAll("\"", ""));
                    } catch (Throwable ignored) {
                        arch = null;
                    }
                } catch (IOException ignored) {

                }
            }
            if(versionAndBuild == null) {
                versionAndBuild = new JavaVersionAndBuild(isExistExtJavaLibrary(jvmDir, "rt") ? 8 : 9, 0);
            }
            JavaVersion resultJavaVersion = new JavaVersion(jvmDir, versionAndBuild.version, versionAndBuild.build, arch, false);
            if (versionAndBuild.version <= 8) {
                resultJavaVersion.enabledJavaFX = isExistExtJavaLibrary(jvmDir, "jfxrt");
            } else {
                resultJavaVersion.enabledJavaFX = tryFindModule(jvmDir, "javafx.base") != null;
                if (!resultJavaVersion.enabledJavaFX)
                    resultJavaVersion.enabledJavaFX = tryFindModule(jvmDir.resolve("jre"), "javafx.base") != null;
            }
            return resultJavaVersion;
        }

        public static boolean isExistExtJavaLibrary(Path jvmDir, String name) {
            Path jrePath = jvmDir.resolve("lib").resolve("ext").resolve(name.concat(".jar"));
            Path jrePathLin = jvmDir.resolve("lib").resolve(name.concat(".jar"));
            Path jdkPath = jvmDir.resolve("jre").resolve("lib").resolve("ext").resolve(name.concat(".jar"));
            Path jdkPathLin = jvmDir.resolve("jre").resolve("lib").resolve(name.concat(".jar"));
            return IOHelper.isFile(jrePath) || IOHelper.isFile(jdkPath) || IOHelper.isFile(jdkPathLin) || IOHelper.isFile(jrePathLin);
        }
    }
}
