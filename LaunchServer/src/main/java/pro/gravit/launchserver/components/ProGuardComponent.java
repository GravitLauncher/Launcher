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
import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.ParseException;
import proguard.ProGuard;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ProGuardComponent extends Component implements AutoCloseable, Reconfigurable {
    private transient static final Logger logger = LogManager.getLogger();
    public String modeAfter = "MainBuild";
    public String dir = "proguard";
    public boolean enabled = true;
    public boolean mappings = true;
    public transient ProguardConf proguardConf;
    private transient LaunchServer launchServer;
    private transient ProGuardBuildTask buildTask;

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
        launchServer.launcherBinary.addAfter((v) -> v.getName().startsWith(modeAfter), buildTask);
    }

    @Override
    public void close() throws Exception {
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
                Configuration proguard_cfg = new Configuration();
                if (!checkJMods(IOHelper.JVM_DIR.resolve("jmods"))) {
                    logger.error("Java path: {} is not JDK! Please install JDK", IOHelper.JVM_DIR);
                }
                Path jfxPath = tryFindOpenJFXPath(IOHelper.JVM_DIR);
                if (checkFXJMods(IOHelper.JVM_DIR.resolve("jmods"))) {
                    logger.debug("JavaFX jmods resolved in JDK path");
                    jfxPath = null;
                } else if (jfxPath != null && checkFXJMods(jfxPath)) {
                    logger.debug("JMods resolved in {}", jfxPath.toString());
                } else {
                    logger.error("JavaFX jmods not found. May be install OpenJFX?");
                    jfxPath = null;
                }
                ConfigurationParser parser = new ConfigurationParser(proguardConf.buildConfig(inputFile, outputJar, jfxPath == null ? new Path[0] : new Path[]{jfxPath}),
                        proguardConf.proguard.toFile(), System.getProperties());
                try {
                    parser.parse(proguard_cfg);
                    ProGuard proGuard = new ProGuard(proguard_cfg);
                    proGuard.execute();
                } catch (Exception e) {
                    logger.error(e);
                }
            } else
                IOHelper.copy(inputFile, outputJar);
            return outputJar;
        }

        @Override
        public boolean allowDelete() {
            return true;
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
        private static final char[] chars = "1aAbBcC2dDeEfF3gGhHiI4jJkKl5mMnNoO6pPqQrR7sStT8uUvV9wWxX0yYzZ".toCharArray();
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

        public String[] buildConfig(Path inputJar, Path outputJar, Path[] jfxPath) {
            List<String> confStrs = new ArrayList<>();
            prepare(false);
            if (component.mappings)
                confStrs.add("-printmapping '" + mappings.toFile().getName() + "'");
            confStrs.add("-obfuscationdictionary '" + words.toFile().getName() + "'");
            confStrs.add("-injar '" + inputJar.toAbsolutePath() + "'");
            confStrs.add("-outjar '" + outputJar.toAbsolutePath() + "'");
            Collections.addAll(confStrs, JAVA9_OPTS);
            if (jfxPath != null) {
                for (Path path : jfxPath) {
                    confStrs.add(String.format("-libraryjars '%s'", path.toAbsolutePath()));
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
            return confStrs.toArray(new String[0]);
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
