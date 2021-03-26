package pro.gravit.launchserver.components;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.launchserver.binary.tasks.LauncherBuildTask;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.*;
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
import java.util.*;

public class ProGuardComponent extends Component implements AutoCloseable, Reconfigurable {
    public String modeAfter = "MainBuild";
    public String dir = "proguard";
    public boolean enabled = true;
    public boolean mappings = true;
    public transient ProguardConf proguardConf;
    @Override
    public void init(LaunchServer launchServer) {
        proguardConf = new ProguardConf(launchServer, this);
        launchServer.launcherBinary.add((v) -> v.getName().startsWith(modeAfter), new ProGuardBuildTask(launchServer, proguardConf, this));
    }

    @Override
    public void close() throws Exception {

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
                ConfigurationParser parser = new ConfigurationParser(proguardConf.buildConfig(inputFile, outputJar),
                        proguardConf.proguard.toFile(), System.getProperties());
                if (JVMHelper.JVM_VERSION >= 9) {
                    Path javaJModsPath = Paths.get(System.getProperty("java.home")).resolve("jmods");
                    if (!IOHelper.exists(javaJModsPath)) {
                        LogHelper.warning("Directory %s not found. It is not good", javaJModsPath);
                    } else {
                        //Find javaFX libraries
                        if (!IOHelper.exists(javaJModsPath.resolve("javafx.base.jmod")))
                            LogHelper.error("javafx.base.jmod not found. Launcher can be assembled incorrectly. Maybe you need to install OpenJFX?");
                        if (!IOHelper.exists(javaJModsPath.resolve("javafx.graphics.jmod")))
                            LogHelper.error("javafx.graphics.jmod not found. Launcher can be assembled incorrectly. Maybe you need to install OpenJFX?");
                        if (!IOHelper.exists(javaJModsPath.resolve("javafx.controls.jmod")))
                            LogHelper.error("javafx.controls.jmod not found. Launcher can be assembled incorrectly. Maybe you need to install OpenJFX?");
                    }
                }
                try {
                    parser.parse(proguard_cfg);
                    ProGuard proGuard = new ProGuard(proguard_cfg);
                    proGuard.execute();
                } catch (ParseException e) {
                    LogHelper.error(e);
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

        public String[] buildConfig(Path inputJar, Path outputJar) {
            List<String> confStrs = new ArrayList<>();
            prepare(false);
            if (component.mappings)
                confStrs.add("-printmapping '" + mappings.toFile().getName() + "'");
            confStrs.add("-obfuscationdictionary '" + words.toFile().getName() + "'");
            confStrs.add("-injar '" + inputJar.toAbsolutePath() + "'");
            confStrs.add("-outjar '" + outputJar.toAbsolutePath() + "'");
            Collections.addAll(confStrs, JVMHelper.JVM_VERSION >= 9 ? JAVA9_OPTS : JAVA8_OPTS);
            srv.launcherBinary.coreLibs.stream()
                    .map(e -> "-libraryjars '" + e.toAbsolutePath().toString() + "'")
                    .forEach(confStrs::add);

            srv.launcherBinary.addonLibs.stream()
                    .map(e -> "-libraryjars '" + e.toAbsolutePath().toString() + "'")
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
                LogHelper.error(e);
            }
        }
    }
}
