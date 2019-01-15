package ru.gravit.launchserver.binary;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class ProguardConf {
    private static final String charsFirst = "aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ";
    private static final String chars = "1aAbBcC2dDeEfF3gGhHiI4jJkKl5mMnNoO6pPqQrR7sStT8uUvV9wWxX0yYzZ";

    private static String generateString(SecureRandom rand, int il) {
        StringBuilder sb = new StringBuilder(il);
        sb.append(charsFirst.charAt(rand.nextInt(charsFirst.length())));
        for (int i = 0; i < il - 1; i++) sb.append(chars.charAt(rand.nextInt(chars.length())));
        return sb.toString();
    }

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

    public String[] buildConfig(Path inputJar, Path outputJar) {
        List<String> confStrs = new ArrayList<>();
        prepare(false);
        if (srv.config.genMappings) confStrs.add("-printmapping \'" + mappings.toFile().getName() + "\'");
        confStrs.add("-obfuscationdictionary \'" + words.toFile().getName() + "\'");
        confStrs.add("-injar \'" + inputJar.toAbsolutePath() + "\'");
        confStrs.add("-outjar \'" + outputJar.toAbsolutePath() + "\'");
        srv.launcherBinary.coreLibs.stream()
                .map(e -> "-libraryjars \'" + e.toAbsolutePath().toString() + "\'")
                .forEach(confStrs::add);
        confStrs.add("-classobfuscationdictionary \'" + words.toFile().getName() + "\'");
        confStrs.add(readConf());
        return confStrs.toArray(new String[0]);
    }

    private void genConfig(boolean force) throws IOException {
        if (IOHelper.exists(config) && !force) return;
        Files.deleteIfExists(config);
        try (OutputStream out = IOHelper.newOutput(config); InputStream in = IOHelper.newInput(IOHelper.getResourceURL("ru/gravit/launchserver/defaults/proguard.cfg"))) {
            IOHelper.transfer(in, out);
        }
    }

    public void genWords(boolean force) throws IOException {
        if (IOHelper.exists(words) && !force) return;
        Files.deleteIfExists(words);
        SecureRandom rand = SecurityHelper.newRandom();
        rand.setSeed(SecureRandom.getSeed(32));
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(IOHelper.newOutput(words), IOHelper.UNICODE_CHARSET))) {
            for (int i = 0; i < Short.MAX_VALUE; i++) out.println(generateString(rand, 24));
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

    private String readConf() {
        return "@".concat(config.toFile().getName());
    }
}
