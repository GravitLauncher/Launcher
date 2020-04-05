package pro.gravit.launchserver.binary;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launcher.serialize.stream.StreamObject;
import pro.gravit.launchserver.binary.tasks.MainBuildTask;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static pro.gravit.utils.helper.IOHelper.UNICODE_CHARSET;
import static pro.gravit.utils.helper.IOHelper.newZipEntry;

public class BuildContext {
    public final ZipOutputStream output;
    public final List<JarFile> readerClassPath;
    public final MainBuildTask task;
    public final HashSet<String> fileList;
    public final HashSet<String> clientModules;

    public BuildContext(ZipOutputStream output, List<JarFile> readerClassPath, MainBuildTask task) {
        this.output = output;
        this.readerClassPath = readerClassPath;
        this.task = task;
        fileList = new HashSet<>(1024);
        clientModules = new HashSet<>();
    }

    public void pushFile(String filename, InputStream inputStream) throws IOException {
        ZipEntry zip = IOHelper.newZipEntry(filename);
        output.putNextEntry(zip);
        IOHelper.transfer(inputStream, output);
        output.closeEntry();
        fileList.add(filename);
    }

    public void pushFile(String filename, StreamObject object) throws IOException {
        ZipEntry zip = IOHelper.newZipEntry(filename);
        output.putNextEntry(zip);
        object.write(new HOutput(output));
        output.closeEntry();
        fileList.add(filename);
    }

    public void pushFile(String filename, Object object, Type type) throws IOException {
        String bytes = Launcher.gsonManager.gson.toJson(object, type);
        pushBytes(filename, bytes.getBytes(UNICODE_CHARSET));
    }

    public void pushDir(Path dir, String targetDir, Map<String, byte[]> hashMap, boolean hidden) throws IOException {
        IOHelper.walk(dir, new RuntimeDirVisitor(output, hashMap, dir, targetDir), hidden);
    }

    public void pushBytes(String filename, byte[] bytes) throws IOException {
        ZipEntry zip = IOHelper.newZipEntry(filename);
        output.putNextEntry(zip);
        output.write(bytes);
        output.closeEntry();
        fileList.add(filename);
    }

    @Deprecated
    public void pushJarFile(ZipInputStream input) throws IOException {
        ZipEntry e = input.getNextEntry();
        while (e != null) {
            if (fileList.contains(e.getName())) {
                e = input.getNextEntry();
                continue;
            }
            output.putNextEntry(IOHelper.newZipEntry(e));
            IOHelper.transfer(input, output);
            fileList.add(e.getName());
            e = input.getNextEntry();
        }
    }

    @Deprecated
    public void pushJarFile(ZipInputStream input, Set<String> blacklist) throws IOException {
        ZipEntry e = input.getNextEntry();
        while (e != null) {
            if (fileList.contains(e.getName()) || blacklist.contains(e.getName())) {
                e = input.getNextEntry();
                continue;
            }
            output.putNextEntry(IOHelper.newZipEntry(e));
            IOHelper.transfer(input, output);
            fileList.add(e.getName());
            e = input.getNextEntry();
        }
    }

    public void pushJarFile(Path jarfile, Predicate<ZipEntry> filter, Predicate<String> needTransform) throws IOException {
        pushJarFile(jarfile.toUri().toURL(), filter, needTransform);
    }

    public void pushJarFile(URL jarfile, Predicate<ZipEntry> filter, Predicate<String> needTransform) throws IOException {
        try (ZipInputStream input = new ZipInputStream(IOHelper.newInput(jarfile))) {
            ZipEntry e = input.getNextEntry();
            while (e != null) {
                String filename = e.getName();
                if (e.isDirectory() || fileList.contains(filename) || filter.test(e)) {
                    e = input.getNextEntry();
                    continue;
                }
                try {
                    output.putNextEntry(IOHelper.newZipEntry(e));
                } catch (ZipException ex) {
                    LogHelper.warning("Write %s failed: %s", filename, ex.getMessage() == null ? "null" : ex.getMessage());
                    e = input.getNextEntry();
                    continue;
                }
                if (filename.endsWith(".class")) {
                    String classname = filename.replace('/', '.').substring(0,
                            filename.length() - ".class".length());
                    if (!needTransform.test(classname)) {
                        IOHelper.transfer(input, output);
                    } else {
                        byte[] bytes = IOHelper.read(input);
                        bytes = task.transformClass(bytes, classname, this);
                        output.write(bytes);
                    }
                } else
                    IOHelper.transfer(input, output);
                fileList.add(filename);
                e = input.getNextEntry();
            }
        }
    }

    private final static class RuntimeDirVisitor extends SimpleFileVisitor<Path> {
        private final ZipOutputStream output;
        private final Map<String, byte[]> hashs;
        private final Path sourceDir;
        private final String targetDir;

        private RuntimeDirVisitor(ZipOutputStream output, Map<String, byte[]> hashs, Path sourceDir, String targetDir) {
            this.output = output;
            this.hashs = hashs;
            this.sourceDir = sourceDir;
            this.targetDir = targetDir;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            String dirName = IOHelper.toString(sourceDir.relativize(dir));
            output.putNextEntry(newEntry(dirName + '/'));
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String fileName = IOHelper.toString(sourceDir.relativize(file));
            if (hashs != null)
                hashs.put(fileName, SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, file));

            // Create zip entry and transfer contents
            output.putNextEntry(newEntry(fileName));
            IOHelper.transfer(file, output);

            // Return result
            return super.visitFile(file, attrs);
        }

        private ZipEntry newEntry(String fileName) {
            return newZipEntry(targetDir + IOHelper.CROSS_SEPARATOR + fileName);
        }
    }
}
