package ru.gravit.launchserver;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.jar.JarFile;

public class StarterAgent {
	
	public static final class StarterVisitor extends SimpleFileVisitor<Path> {
		private Instrumentation inst;

		public StarterVisitor(Instrumentation inst) {
			this.inst = inst;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (file.toFile().getName().endsWith(".jar")) inst.appendToSystemClassLoaderSearch(new JarFile(file.toFile()));
			return super.visitFile(file, attrs);
		}
	}
	public static void premain(String agentArgument, Instrumentation inst) {
		try {
			Files.walkFileTree(Paths.get("libraries"), Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new StarterVisitor(inst));
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
}
