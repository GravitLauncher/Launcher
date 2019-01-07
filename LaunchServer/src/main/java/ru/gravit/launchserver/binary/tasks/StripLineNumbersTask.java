package ru.gravit.launchserver.binary.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.asm.ClassMetadataReader;
import ru.gravit.launchserver.asm.SafeClassWriter;
import ru.gravit.utils.helper.IOHelper;

public class StripLineNumbersTask implements LauncherBuildTask {
	private final LaunchServer server;
	
	public StripLineNumbersTask(LaunchServer server) {
		this.server = server;
	}

	@Override
	public String getName() {
		return "Strip debug task";
	}

	@Override
	public Path process(Path inputFile) throws IOException {
		Path out = server.dir.resolve(server.config.projectName + "-stripped.jar");
		try (ClassMetadataReader reader = new ClassMetadataReader()) {
			reader.getCp().add(new JarFile(inputFile.toFile()));
			try (ZipInputStream input = IOHelper.newZipInput(inputFile);
					ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(out))) {
            	ZipEntry e = input.getNextEntry();
            	while (e != null) {
                	String filename = e.getName();
                	output.putNextEntry(IOHelper.newZipEntry(e));
                	if (filename.endsWith(".class")) {
                    	byte[] bytes = null;
                    	try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048)) {
                        	IOHelper.transfer(input, outputStream);
                        	bytes = outputStream.toByteArray();
                    	}
                    	output.write(classFix(bytes, reader));
                	} else
                    	IOHelper.transfer(input, output);
                	e = input.getNextEntry();
            	}
        	}
		}
		return out;
	}

	private static byte[] classFix(byte[] bytes, ClassMetadataReader reader) {
		ClassReader cr = new ClassReader(bytes);
		ClassWriter cw = new SafeClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cr.accept(cw, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		return cw.toByteArray();
	}

	@Override
	public boolean allowDelete() {
		return true;
	}

}
