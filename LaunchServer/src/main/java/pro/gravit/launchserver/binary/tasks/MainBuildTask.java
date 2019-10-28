package pro.gravit.launchserver.binary.tasks;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import pro.gravit.launcher.AutogenConfig;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.SecureAutogenConfig;
import pro.gravit.launcher.SimpleAutogenConfig;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.asm.ClassMetadataReader;
import pro.gravit.launchserver.asm.ConfigGenerator;
import pro.gravit.launchserver.binary.BuildContext;
import pro.gravit.launchserver.binary.LauncherConfigurator;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.cert.CertificateEncodingException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static pro.gravit.utils.helper.IOHelper.newZipEntry;

public class MainBuildTask implements LauncherBuildTask {
    private final LaunchServer server;
    public final ClassMetadataReader reader;

    private final class RuntimeDirVisitor extends SimpleFileVisitor<Path> {
        private final ZipOutputStream output;
        private final Map<String, byte[]> runtime;

        private RuntimeDirVisitor(ZipOutputStream output, Map<String, byte[]> runtime) {
            this.output = output;
            this.runtime = runtime;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            String dirName = IOHelper.toString(server.launcherBinary.runtimeDir.relativize(dir));
            output.putNextEntry(newEntry(dirName + '/'));
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String fileName = IOHelper.toString(server.launcherBinary.runtimeDir.relativize(file));
            runtime.put(fileName, SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, file));

            // Create zip entry and transfer contents
            output.putNextEntry(newEntry(fileName));
            IOHelper.transfer(file, output);

            // Return result
            return super.visitFile(file, attrs);
        }
    }

    private final class GuardDirVisitor extends SimpleFileVisitor<Path> {
        private final ZipOutputStream output;
        private final Map<String, byte[]> guard;

        private GuardDirVisitor(ZipOutputStream output, Map<String, byte[]> guard) {
            this.output = output;
            this.guard = guard;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            String dirName = IOHelper.toString(server.launcherBinary.guardDir.relativize(dir));
            output.putNextEntry(newGuardEntry(dirName + '/'));
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String fileName = IOHelper.toString(server.launcherBinary.guardDir.relativize(file));
            guard.put(fileName, SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, file));

            // Create zip entry and transfer contents
            output.putNextEntry(newGuardEntry(fileName));
            IOHelper.transfer(file, output);

            // Return result
            return super.visitFile(file, attrs);
        }
    }

    private static ZipEntry newEntry(String fileName) {
        return newZipEntry(Launcher.RUNTIME_DIR + IOHelper.CROSS_SEPARATOR + fileName);
    }

    private static ZipEntry newGuardEntry(String fileName) {
        return newZipEntry(Launcher.GUARD_DIR + IOHelper.CROSS_SEPARATOR + fileName);
    }

    public MainBuildTask(LaunchServer srv) {
        server = srv;
        reader = new ClassMetadataReader();
    }

    @Override
    public String getName() {
        return "MainBuild";
    }

    @Override
    public Path process(Path inputJar) throws IOException {
        Path outputJar = server.launcherBinary.nextPath("main");
        try (ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(outputJar))) {
            ClassNode cn = new ClassNode();
            new ClassReader(IOHelper.getResourceBytes(AutogenConfig.class.getName().replace('.', '/').concat(".class"))).accept(cn, 0);
            LauncherConfigurator launcherConfigurator = new LauncherConfigurator(cn);
            ClassNode cn1 = new ClassNode();
            new ClassReader(IOHelper.getResourceBytes(SecureAutogenConfig.class.getName().replace('.', '/').concat(".class"))).accept(cn1, 0);
            ConfigGenerator secureConfigurator = new ConfigGenerator(cn1);
            ClassNode cn2 = new ClassNode();
            new ClassReader(IOHelper.getResourceBytes(SimpleAutogenConfig.class.getName().replace('.', '/').concat(".class"))).accept(cn2, 0);
            ConfigGenerator runtimeConfigurator = new ConfigGenerator(cn2);
            BuildContext context = new BuildContext(output, launcherConfigurator, this);
            server.buildHookManager.hook(context);
            launcherConfigurator.setStringField("address", server.config.netty.address);
            launcherConfigurator.setStringField("projectname", server.config.projectName);
            launcherConfigurator.setStringField("secretKeyClient", SecurityHelper.randomStringAESKey());
            launcherConfigurator.setIntegerField("clientPort", 32148 + SecurityHelper.newRandom().nextInt(512));
            launcherConfigurator.setStringField("guardType", server.config.launcher.guardType);
            launcherConfigurator.setBooleanField("isWarningMissArchJava", server.config.launcher.warningMissArchJava);
            launcherConfigurator.setEnv(server.config.env);
            launcherConfigurator.setStringField("passwordEncryptKey", server.runtime.passwordEncryptKey);
            List<byte[]> certificates = Arrays.stream(server.certificateManager.trustManager.getTrusted()).map(e -> {
                try {
                    return e.getEncoded();
                } catch (CertificateEncodingException e2) {
                    LogHelper.error(e2);
                    return new byte[0];
                }
            }).collect(Collectors.toList());
            if(!server.config.sign.enabled)
            {
                CertificateAutogenTask task = server.launcherBinary.getTaskByClass(CertificateAutogenTask.class).get();
                try {
                    certificates.add(task.certificate.getEncoded());
                } catch (CertificateEncodingException e) {
                    throw new InternalError(e);
                }
            }
            secureConfigurator.setByteArrayListField("certificates", certificates);
            String launcherSalt = SecurityHelper.randomStringToken();
            byte[] launcherSecureHash = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256,
                    server.runtime.clientCheckSecret.concat(".").concat(launcherSalt));
            launcherConfigurator.setStringField("secureCheckHash", Base64.getEncoder().encodeToString(launcherSecureHash));
            launcherConfigurator.setStringField("secureCheckSalt", launcherSalt);
            //LogHelper.debug("[checkSecure] %s: %s", launcherSalt, Arrays.toString(launcherSecureHash));
            if (server.runtime.oemUnlockKey == null) server.runtime.oemUnlockKey = SecurityHelper.randomStringToken();
            launcherConfigurator.setStringField("oemUnlockKey", server.runtime.oemUnlockKey);
            server.buildHookManager.registerAllClientModuleClass(launcherConfigurator);
            reader.getCp().add(new JarFile(inputJar.toFile()));
            server.launcherBinary.coreLibs.forEach(e -> {
                try {
                    reader.getCp().add(new JarFile(e.toFile()));
                } catch (IOException e1) {
                    LogHelper.error(e1);
                }
            });
            String zPath = launcherConfigurator.getZipEntryPath();
            String sPath = secureConfigurator.getZipEntryPath();
            String rPath = runtimeConfigurator.getZipEntryPath();
            try (ZipInputStream input = new ZipInputStream(IOHelper.newInput(inputJar))) {
                ZipEntry e = input.getNextEntry();
                while (e != null) {
                    String filename = e.getName();
                    if (server.buildHookManager.isContainsBlacklist(filename) || e.isDirectory() || zPath.equals(filename) || sPath.equals(filename) || rPath.equals(filename)) {
                        e = input.getNextEntry();
                        continue;
                    }
                    try {
                        output.putNextEntry(IOHelper.newZipEntry(e));
                    } catch (ZipException ex) {
                        LogHelper.error(ex);
                        e = input.getNextEntry();
                        continue;
                    }
                    if (filename.endsWith(".class")) {
                        String classname = filename.replace('/', '.').substring(0,
                                filename.length() - ".class".length());
                        byte[] bytes;
                        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048)) {
                            IOHelper.transfer(input, outputStream);
                            bytes = outputStream.toByteArray();
                        }
                        bytes = server.buildHookManager.classTransform(bytes, classname, this);
                        output.write(bytes);
                    } else
                        IOHelper.transfer(input, output);
                    context.fileList.add(filename);
                    e = input.getNextEntry();
                }
            }
            // write additional classes
            for (Map.Entry<String, byte[]> ent : server.buildHookManager.getIncludeClass().entrySet()) {
                output.putNextEntry(newZipEntry(ent.getKey().replace('.', '/').concat(".class")));
                output.write(server.buildHookManager.classTransform(ent.getValue(), ent.getKey(), this));
            }
            // Create launcher config file
            Map<String, byte[]> runtime = new HashMap<>(256);
            if (server.buildHookManager.buildRuntime()) {
                IOHelper.walk(server.launcherBinary.runtimeDir, new RuntimeDirVisitor(output, runtime), false);
                IOHelper.walk(server.launcherBinary.guardDir, new GuardDirVisitor(output, runtime), false);
            }
            runtimeConfigurator.setStringByteArrMapField("entries", runtime);
            runtimeConfigurator.setByteArrayField("key", server.publicKey.getEncoded());
            // Write launcher config file
            output.putNextEntry(newZipEntry(rPath));
            output.write(runtimeConfigurator.getBytecode(reader));
            ZipEntry e = newZipEntry(zPath);
            output.putNextEntry(e);
            output.write(launcherConfigurator.getBytecode(reader));

            e = newZipEntry(sPath);
            output.putNextEntry(e);
            output.write(secureConfigurator.getBytecode(reader));
        }
        reader.close();
        return outputJar;
    }

    @Override
    public boolean allowDelete() {
        return true;
    }
}
