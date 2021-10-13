package pro.gravit.launchserver.binary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launcher.serialize.stream.StreamObject;
import pro.gravit.launchserver.binary.tasks.MainBuildTask;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
        ZipEntry zip = IOHelper.newZipEntry(filename);
        output.putNextEntry(zip);
        try (BufferedWriter w = IOHelper.newWriter(IOHelper.nonClosing(output))) {
            Launcher.gsonManager.gson.toJson(object, type);
        }
        output.closeEntry();
        fileList.add(filename);
        pushBytes(filename, IOHelper.encode(Launcher.gsonManager.gson.toJson(object, type)));
    }

    public void pushDir(Path dir, String targetDir, Map<String, byte[]> hashMap, boolean hidden) throws IOException {
        IOHelper.walk(dir, new RuntimeDirVisitor(output, hashMap, dir, targetDir), hidden);
    }

    public void pushEncryptedDir(Path dir, String targetDir, String aesHexKey, Map<String, byte[]> hashMap, boolean hidden) throws IOException {
        IOHelper.walk(dir, new EncryptedRuntimeDirVisitor(output, aesHexKey, hashMap, dir, targetDir), hidden);
    }

    public void pushBytes(String filename, byte[] bytes) throws IOException {
        ZipEntry zip = IOHelper.newZipEntry(filename);
        output.putNextEntry(zip);
        output.write(bytes);
        output.closeEntry();
        fileList.add(filename);
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
                output.putNextEntry(IOHelper.newZipEntry(e));
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

    private final static class EncryptedRuntimeDirVisitor extends SimpleFileVisitor<Path> {
        private final ZipOutputStream output;
        private final Map<String, byte[]> hashs;
        private final Path sourceDir;
        private final String targetDir;
        private final SecretKeySpec sKeySpec;
        private final IvParameterSpec iKeySpec;
        private final transient Logger logger = LogManager.getLogger();

        private EncryptedRuntimeDirVisitor(ZipOutputStream output, String aesKey, Map<String, byte[]> hashs, Path sourceDir, String targetDir) {
            this.output = output;
            this.hashs = hashs;
            this.sourceDir = sourceDir;
            this.targetDir = targetDir;
            try {
                byte[] key = SecurityHelper.fromHex(aesKey);
                byte[] compatKey = SecurityHelper.getAESKey(key);
                sKeySpec = new SecretKeySpec(compatKey, "AES/CBC/PKCS5Padding");
                iKeySpec = new IvParameterSpec(IOHelper.encode("8u3d90ikr7o67lsq"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            byte[] digest = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, file);
            String fileName = IOHelper.toString(sourceDir.relativize(file));
            if (hashs != null) {
                hashs.put(fileName, digest);
            }
            // Create zip entry and transfer contents
            try {
                output.putNextEntry(newEntry(SecurityHelper.toHex(digest)));
            } catch (ZipException e) {
                return super.visitFile(file, attrs); // fix duplicate files
            }


            Cipher cipher = null;
            try {
                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, sKeySpec, iKeySpec);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e);
            }
            try (OutputStream stream = new CipherOutputStream(new NoCloseOutputStream(output), cipher)) {
                IOHelper.transfer(file, stream);
            }

            // Return result
            return super.visitFile(file, attrs);
        }

        private ZipEntry newEntry(String fileName) {
            return newZipEntry(targetDir + IOHelper.CROSS_SEPARATOR + fileName);
        }

        private static class NoCloseOutputStream extends OutputStream {
            private final OutputStream stream;

            private NoCloseOutputStream(OutputStream stream) {
                this.stream = stream;
            }

            @Override
            public void write(int i) throws IOException {
                stream.write(i);
            }

            @Override
            public void write(byte[] b) throws IOException {
                stream.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                stream.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                stream.flush();
            }
        }
    }
}
