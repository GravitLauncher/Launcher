package pro.gravit.launchserver.binary;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProguardConf {
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

    public ProguardConf(LaunchServer srv) {
        proguard = srv.dir.resolve("proguard");
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
        if (srv.config.launcher.proguardGenMappings)
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
