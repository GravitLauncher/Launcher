package pro.gravit.launchserver.binary.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.asm.ClassMetadataReader;
import pro.gravit.launchserver.asm.SafeClassWriter;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class AdditionalFixesApplyTask implements LauncherBuildTask {
    private final LaunchServer server;

    public AdditionalFixesApplyTask(LaunchServer server) {
        this.server = server;
    }

    @Override
    public String getName() {
        return "AdditionalFixesApply";
    }

    @Override
    public Path process(Path inputFile) throws IOException {
        Path out = server.launcherBinary.nextPath("post-fixed");
        try (ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(out))) {
            apply(inputFile, inputFile, output, server, (e) -> false);
        }
        return out;
    }

    public static void apply(Path inputFile, Path addFile, ZipOutputStream output, LaunchServer srv, Predicate<ZipEntry> excluder) throws IOException {
        try (ClassMetadataReader reader = new ClassMetadataReader()) {
            reader.getCp().add(new JarFile(inputFile.toFile()));
            List<JarFile> libs = srv.launcherBinary.coreLibs.stream().map(e -> {
                try {
                    return new JarFile(e.toFile());
                } catch (IOException e1) {
                    throw new RuntimeException(e1);
                }
            }).collect(Collectors.toList());
            libs.addAll(srv.launcherBinary.addonLibs.stream().map(e -> {
                try {
                    return new JarFile(e.toFile());
                } catch (IOException e1) {
                    throw new RuntimeException(e1);
                }
            }).collect(Collectors.toList()));
            try (ZipInputStream input = IOHelper.newZipInput(addFile)) {
                ZipEntry e = input.getNextEntry();
                while (e != null) {
                    if (e.isDirectory() || excluder.test(e)) {
                        e = input.getNextEntry();
                        continue;
                    }
                    String filename = e.getName();
                    output.putNextEntry(IOHelper.newZipEntry(e));
                    if (filename.endsWith(".class")) {
                        byte[] bytes;
                        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048)) {
                            IOHelper.transfer(input, outputStream);
                            bytes = outputStream.toByteArray();
                        }
                        try {
                        	bytes = classFix(bytes, reader, srv.config.launcher.stripLineNumbers);
                        } catch (Throwable t) {
                        	LogHelper.error(e);
                        }
                        output.write(bytes);
                    } else
                        IOHelper.transfer(input, output);
                    e = input.getNextEntry();
                }
            }
        }
    }

    private static byte[] classFix(byte[] bytes, ClassMetadataReader reader, boolean stripNumbers) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, stripNumbers ? (ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES) : ClassReader.SKIP_FRAMES);
        ClassWriter cw = new SafeClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    @Override
    public boolean allowDelete() {
        return true;
    }

}
