package ru.gravit.launchserver.binary.tasks;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.IOHelper;

public class AttachJarsTask implements LauncherBuildTask {
    private static final class ListFileVisitor extends SimpleFileVisitor<Path> {
        private final List<Path> lst;

        private ListFileVisitor(List<Path> lst) {
            this.lst = lst;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toFile().getName().endsWith(".jar"))
                lst.add(file);
            return super.visitFile(file, attrs);
        }
    }
	
	private final LaunchServer srv;
	private final List<Path> jars;
	private final List<String> exclusions;

	public AttachJarsTask(LaunchServer srv) {
		this.srv = srv;
		jars = new ArrayList<>();
		exclusions = new ArrayList<>();
	}

	@Override
	public String getName() {
		return "AttachJars";
	}

	@Override
	public Path process(Path inputFile) throws IOException {
		Path outputFile = srv.launcherBinary.nextPath("attached");
		try (ZipInputStream input = IOHelper.newZipInput(inputFile);
				ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(outputFile))) {
			ZipEntry e = input.getNextEntry();
			while (e != null) {
				output.putNextEntry(IOHelper.newZipEntry(e));
				IOHelper.transfer(input, output);
				e = input.getNextEntry();
			}
			List<Path> coreAttach = new ArrayList<>();
			IOHelper.walk(srv.launcherLibraries, new ListFileVisitor(coreAttach), true);
			attach(output, coreAttach);
			attach(output, jars);
		}
		return outputFile;
	}

	private void attach(ZipOutputStream output, List<Path> lst) throws IOException {
		for (Path p : lst) {
			try (ZipInputStream input = IOHelper.newZipInput(p)) {
			ZipEntry e = input.getNextEntry();
				while (e != null) {
					String filename = e.getName();
					if (exclusions.stream().noneMatch(filename::startsWith)) {
						output.putNextEntry(IOHelper.newZipEntry(e));
						IOHelper.transfer(input, output);
					}
					e = input.getNextEntry();
				}
			}
		}
	}

	@Override
	public boolean allowDelete() {
		return true;
	}

	public List<Path> getJars() {
		return jars;
	}

	public List<String> getExclusions() {
		return exclusions;
	}
}
