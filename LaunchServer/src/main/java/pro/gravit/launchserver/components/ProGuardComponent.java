package pro.gravit.launchserver.components;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.launchserver.binary.tasks.LauncherBuildTask;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.utils.helper.UnpackHelper;

import java.io.*;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ProGuardComponent extends Component implements AutoCloseable, Reconfigurable {
    private static final Logger logger = LogManager.getLogger();
    public String modeAfter = "MainBuild";
    public String dir = "proguard";
    public List<String> jvmArgs = new ArrayList<>();
    public boolean enabled = true;
    public boolean mappings = true;
    public transient ProguardConf proguardConf;
    private transient LaunchServer launchServer;
    private transient ProGuardBuildTask buildTask;
    private transient ProGuardMultiReleaseFixer fixerTask;

    public ProGuardComponent() {
        this.jvmArgs.add("-Xmx512M");
    }

    public static boolean checkFXJMods(Path path) {
        if (!IOHelper.exists(path.resolve("javafx.base.jmod")))
            return false;
        if (!IOHelper.exists(path.resolve("javafx.graphics.jmod")))
            return false;
        return IOHelper.exists(path.resolve("javafx.controls.jmod"));
    }

    public static boolean checkJMods(Path path) {
        return IOHelper.exists(path.resolve("java.base.jmod"));
    }

    public static Path tryFindOpenJFXPath(Path jvmDir) {
        String dirName = jvmDir.getFileName().toString();
        Path parent = jvmDir.getParent();
        if (parent == null) return null;
        Path archJFXPath = parent.resolve(dirName.replace("openjdk", "openjfx")).resolve("jmods");
        if (Files.isDirectory(archJFXPath)) {
            return archJFXPath;
        }
        Path arch2JFXPath = parent.resolve(dirName.replace("jdk", "openjfx")).resolve("jmods");
        if (Files.isDirectory(arch2JFXPath)) {
            return arch2JFXPath;
        }
        if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
            Path debianJfxPath = Paths.get("/usr/share/openjfx/jmods");
            if (Files.isDirectory(debianJfxPath)) {
                return debianJfxPath;
            }
        }
        return null;
    }

    @Override
    public void init(LaunchServer launchServer) {
        this.launchServer = launchServer;
        proguardConf = new ProguardConf(launchServer, this);
        this.buildTask = new ProGuardBuildTask(launchServer, proguardConf, this);
        this.fixerTask = new ProGuardMultiReleaseFixer(launchServer, this, "ProGuard.".concat(componentName));
        launchServer.launcherBinary.addAfter((v) -> v.getName().startsWith(modeAfter), buildTask);
        launchServer.launcherBinary.addAfter((v) -> v.getName().equals("ProGuard.".concat(componentName)), fixerTask);
    }

    @Override
    public void close() {
        if (launchServer != null && buildTask != null) {
            launchServer.launcherBinary.tasks.remove(buildTask);
        }
    }

    @Override
    public Map<String, Command> getCommands() {
        Map<String, Command> commands = defaultCommandsMap();
        commands.put("reset", new SubCommand("[]", "reset proguard config") {
            @Override
            public void invoke(String... args) throws Exception {
                proguardConf.prepare(true);
                Files.deleteIfExists(proguardConf.mappings);
            }
        });
        commands.put("regen", new SubCommand("[]", "regenerate proguard dictionary") {
            @Override
            public void invoke(String... args) throws Exception {
                proguardConf.genWords(true);
            }
        });
        commands.put("clean", new SubCommand("[]", "clean proguard mappings") {
            @Override
            public void invoke(String... args) throws Exception {
                proguardConf.prepare(true);
                Files.deleteIfExists(proguardConf.mappings);
            }
        });
        return null;
    }

    public static class ProGuardMultiReleaseFixer implements LauncherBuildTask {
        private final LaunchServer server;
        private final ProGuardComponent component;
        private final String proguardTaskName;

        public ProGuardMultiReleaseFixer(LaunchServer server, ProGuardComponent component, String proguardTaskName) {
            this.server = server;
            this.component = component;
            this.proguardTaskName = proguardTaskName;
        }

        @Override
        public String getName() {
            return "ProGuardMultiReleaseFixer.".concat(component.componentName);
        }

        @Override
        public Path process(Path inputFile) throws IOException {
            if (!component.enabled) {
                return inputFile;
            }
            LauncherBuildTask task = server.launcherBinary.getTaskBefore((x) -> proguardTaskName.equals(x.getName())).get();
            Path lastPath = server.launcherBinary.nextPath(task);
            if(Files.notExists(lastPath)) {
                logger.error("{} not exist. Multi-Release JAR fix not applied!", lastPath);
                return inputFile;
            }
            Path outputPath = server.launcherBinary.nextPath(this);
            try(ZipOutputStream output = new ZipOutputStream(new FileOutputStream(outputPath.toFile()))) {
                try(ZipInputStream input = new ZipInputStream(new FileInputStream(inputFile.toFile()))) {
                    ZipEntry entry = input.getNextEntry();
                    while(entry != null) {
                        ZipEntry newEntry = new ZipEntry(entry.getName());
                        output.putNextEntry(newEntry);
                        input.transferTo(output);
                        entry = input.getNextEntry();
                    }
                }
                try(ZipInputStream input = new ZipInputStream(new FileInputStream(lastPath.toFile()))) {
                    ZipEntry entry = input.getNextEntry();
                    while(entry != null) {
                        if(!entry.getName().startsWith("META-INF/versions")) {
                            entry = input.getNextEntry();
                            continue;
                        }
                        ZipEntry newEntry = new ZipEntry(entry.getName());
                        output.putNextEntry(newEntry);
                        input.transferTo(output);
                        entry = input.getNextEntry();
                    }
                }
            }
            return outputPath;
        }
    }

    public static class ProGuardBuildTask implements LauncherBuildTask {
        private final LaunchServer server;
        private final ProGuardComponent component;
        private final ProguardConf proguardConf;

        public ProGuardBuildTask(LaunchServer server, ProguardConf conf, ProGuardComponent component) {
            this.server = server;
            this.component = component;
            this.proguardConf = conf;
        }

        @Override
        public String getName() {
            return "ProGuard.".concat(component.componentName);
        }

        @Override
        public Path process(Path inputFile) throws IOException {
            Path outputJar = server.launcherBinary.nextLowerPath(this);
            if (component.enabled) {
                if (!checkJMods(IOHelper.JVM_DIR.resolve("jmods"))) {
                    throw new RuntimeException("Java path: %s is not JDK! Please install JDK".formatted(IOHelper.JVM_DIR));
                }
                Path jfxPath = tryFindOpenJFXPath(IOHelper.JVM_DIR);
                if (checkFXJMods(IOHelper.JVM_DIR.resolve("jmods"))) {
                    logger.debug("JavaFX jmods resolved in JDK path");
                    jfxPath = null;
                } else if (jfxPath != null && checkFXJMods(jfxPath)) {
                    logger.debug("JMods resolved in {}", jfxPath.toString());
                } else {
                    throw new RuntimeException("JavaFX jmods not found. May be install OpenJFX?");
                }
                try {
                    List<String> args = new ArrayList<>();
                    args.add(IOHelper.resolveJavaBin(IOHelper.JVM_DIR).toAbsolutePath().toString());
                    args.addAll(component.jvmArgs);
                    args.add("-cp");
                    try(Stream<Path> files = Files.walk(server.librariesDir, FileVisitOption.FOLLOW_LINKS)) {
                        args.add(files
                                .filter(e -> e.getFileName().toString().endsWith(".jar"))
                                .map(path -> path.toAbsolutePath().toString())
                                .collect(Collectors.joining(File.pathSeparator))
                        );
                    }
                    args.add("proguard.ProGuard");
                    proguardConf.buildConfig(args, inputFile, outputJar, jfxPath == null ? new Path[0] : new Path[]{jfxPath});

                    Process process = new ProcessBuilder()
                            .command(args)
                            .inheritIO()
                            .directory(proguardConf.proguard.toFile())
                            .start();

                    try {
                        process.waitFor();
                    } catch (InterruptedException ignored) {

                    }
                    if (process.exitValue() != 0) {
                        throw new RuntimeException("ProGuard process return %d".formatted(process.exitValue()));
                    }
                } catch (Exception e) {
                    logger.error(e);
                }
            } else
                IOHelper.copy(inputFile, outputJar);
            return outputJar;
        }
    }

    public static class ProguardConf {
        public static final String[] JAVA9_OPTS = new String[]{
                "-libraryjars '<java.home>/jmods/'"
        };
        public static final String[] JAVA8_OPTS = new String[]{
                "-libraryjars '<java.home>/lib/rt.jar'",
                "-libraryjars '<java.home>/lib/jce.jar'",
                "-libraryjars '<java.home>/lib/ext/nashorn.jar'",
                "-libraryjars '<java.home>/lib/ext/jfxrt.jar'"
        };
        private static final char[] chars = "1aAbBcC2dDeEfF3gGhHiI4jJkKlL5mMnNoO6pPqQrR7sStT8uUvV9wWxX0yYzZ".toCharArray();
        public final Path proguard;
        public final Path config;
        public final Path mappings;
        public final Path words;
        public transient final LaunchServer srv;
        private transient final ProGuardComponent component;

        public ProguardConf(LaunchServer srv, ProGuardComponent component) {
            this.component = component;
            this.proguard = srv.dir.resolve(component.dir);
            config = proguard.resolve("proguard.config");
            mappings = proguard.resolve("mappings.pro");
            words = proguard.resolve("random.pro");
            this.srv = srv;
        }

        private static String generateString(SecureRandom rand, String lowString, String upString, int il) {
            StringBuilder sb = new StringBuilder(Math.max(il, lowString.length()));
            for (int i = 0; i < lowString.length(); ++i) {
                sb.append(rand.nextBoolean() ? lowString.charAt(i) : upString.charAt(i));
            }
            int toI = il - lowString.length();
            for (int i = 0; i < toI; i++) sb.append(chars[rand.nextInt(chars.length)]);
            return sb.toString();
        }

        public void buildConfig(List<String> confStrs, Path inputJar, Path outputJar, Path[] jfxPath) {
            prepare(false);
            if (component.mappings)
                confStrs.add("-printmapping '" + mappings.toFile().getName() + "'");
            confStrs.add("-obfuscationdictionary '" + words.toFile().getName() + "'");
            confStrs.add("-injar '" + inputJar.toAbsolutePath() + "'");
            confStrs.add("-outjar '" + outputJar.toAbsolutePath() + "'");
            Collections.addAll(confStrs, JAVA9_OPTS);
            if (jfxPath != null) {
                for (Path path : jfxPath) {
                    confStrs.add("-libraryjars '%s'".formatted(path.toAbsolutePath()));
                }
            }
            srv.launcherBinary.coreLibs.stream()
                    .map(e -> "-libraryjars '" + e.toAbsolutePath() + "'")
                    .forEach(confStrs::add);

            srv.launcherBinary.addonLibs.stream()
                    .map(e -> "-libraryjars '" + e.toAbsolutePath() + "'")
                    .forEach(confStrs::add);
            confStrs.add("-classobfuscationdictionary '" + words.toFile().getName() + "'");
            confStrs.add("@".concat(config.toFile().getName()));
        }

        private void genConfig(boolean force) throws IOException {
            if (IOHelper.exists(config) && !force) return;
            Files.deleteIfExists(config);
            UnpackHelper.unpack(IOHelper.getResourceURL("pro/gravit/launchserver/defaults/proguard.cfg"), config);
        }

        public void genWords(boolean force) throws IOException {
            if (IOHelper.exists(words) && !force) return;
            Files.deleteIfExists(words);
            SecureRandom rand = SecurityHelper.newRandom();
            rand.setSeed(SecureRandom.getSeed(32));
            try (PrintWriter out = new PrintWriter(new OutputStreamWriter(IOHelper.newOutput(words), IOHelper.UNICODE_CHARSET))) {
                String projectName = srv.config.projectName.replaceAll("\\W", "");
                String lowName = projectName.toLowerCase();
                String upName = projectName.toUpperCase();
                for (int i = 0; i < Short.MAX_VALUE; i++) out.println(generateString(rand, lowName, upName, 14));
            }
        }

        public void prepare(boolean force) {
            try {
                IOHelper.createParentDirs(config);
                genWords(force);
                genConfig(force);
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }
}
