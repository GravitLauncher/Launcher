package ru.gravit.launchserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;

public class ProguardConf {
	private static final String charsFirst = "aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ";
	private static final String chars = "1aAbBcC2dDeEfF3gGhHiI4jJkKl5mMnNoO6pPqQrR7sStT8uUvV9wWxX0yYzZ";
	private static String generateString(SecureRandom rand, int il) {
		StringBuffer sb = new StringBuffer(il);
		sb.append(charsFirst.charAt(rand.nextInt(charsFirst.length())));
		for (int i = 0; i < il - 1; i++) sb.append(chars.charAt(rand.nextInt(chars.length())));
		return sb.toString();
	}
	private final LaunchServer srv;
	public final Path proguard;
	public final Path config;
	public final Path mappings;
	public final Path words;
	public final Set<String> confStrs;
	
	public ProguardConf(LaunchServer srv) {
		this.srv = srv;
		proguard =  this.srv.dir.resolve("proguard");
		config = proguard.resolve("proguard.config");
		mappings = proguard.resolve("mappings.pro");
		words = proguard.resolve("random.pro");
		confStrs = new HashSet<>();
		prepare(false);
		confStrs.add(readConf());
		if (this.srv.config.genMappings) confStrs.add("-printmapping \'" + mappings.toFile().getName() + "\'");
		confStrs.add("-obfuscationdictionary \'" + words.toFile().getName() + "\'");
		confStrs.add("-classobfuscationdictionary \'" + words.toFile().getName() + "\'");

	}

	private void genConfig(boolean force) throws IOException {
		if (IOHelper.exists(config) && !force) return;
		Files.deleteIfExists(config);
		config.toFile().createNewFile();
		try (OutputStream out = IOHelper.newOutput(config); InputStream in = IOHelper.newInput(IOHelper.getResourceURL("ru/gravit/launchserver/defaults/proguard.cfg"))) {
			IOHelper.transfer(in, out);
		}
	}

	public void genWords(boolean force) throws IOException {
		if (IOHelper.exists(words) && !force) return;
		Files.deleteIfExists(words);
		words.toFile().createNewFile();
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
