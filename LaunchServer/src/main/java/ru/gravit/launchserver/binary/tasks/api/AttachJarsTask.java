package ru.gravit.launchserver.binary.tasks.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.binary.tasks.LauncherBuildTask;
import ru.gravit.utils.helper.IOHelper;

public class AttachJarsTask implements LauncherBuildTask {

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
			attach(output);
		}
		return outputFile;
	}

	private void attach(ZipOutputStream output) throws IOException {
		for (Path p : jars) {
			try (ZipInputStream input = IOHelper.newZipInput(p)) {
			ZipEntry e = input.getNextEntry();
				while (e != null) {
					String filename = e.getName();
					if (exclusions.stream().noneMatch(exc -> filename.startsWith(exc))) {
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
