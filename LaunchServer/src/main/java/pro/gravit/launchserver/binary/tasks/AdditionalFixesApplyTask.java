package pro.gravit.launchserver.binary.tasks;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.asm.ClassMetadataReader;
import pro.gravit.launchserver.asm.SafeClassWriter;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class AdditionalFixesApplyTask implements LauncherBuildTask {
    private final LaunchServer server;

    public AdditionalFixesApplyTask(LaunchServer server) {
        this.server = server;
    }

    public static void apply(Path inputFile, Path addFile, ZipOutputStream output, LaunchServer srv, Predicate<ZipEntry> excluder, boolean needFixes) throws IOException {
        try (ClassMetadataReader reader = new ClassMetadataReader()) {
            reader.getCp().add(new JarFile(inputFile.toFile()));
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
                        if (needFixes) {
                            bytes = classFix(input, reader, srv.config.launcher.stripLineNumbers);
                            output.write(bytes);
                        } else
                            IOHelper.transfer(input, output);
                    } else
                        IOHelper.transfer(input, output);
                    e = input.getNextEntry();
                }
            }
        }
    }

    private static byte[] classFix(InputStream input, ClassMetadataReader reader, boolean stripNumbers) throws IOException {
        ClassReader cr = new ClassReader(input);
        ClassNode cn = new ClassNode();
        cr.accept(cn, stripNumbers ? (ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES) : ClassReader.SKIP_FRAMES);
        ClassWriter cw = new SafeClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    @Override
    public String getName() {
        return "AdditionalFixesApply";
    }

    @Override
    public Path process(Path inputFile) throws IOException {
        Path out = server.launcherBinary.nextPath("post-fixed");
        try (ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(out))) {
            apply(inputFile, inputFile, output, server, (e) -> false, true);
        }
        return out;
    }

    @Override
    public boolean allowDelete() {
        return true;
    }

}
