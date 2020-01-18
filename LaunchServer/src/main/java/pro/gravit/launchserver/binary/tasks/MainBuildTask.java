package pro.gravit.launchserver.binary.tasks;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import pro.gravit.launcher.AutogenConfig;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.SecureAutogenConfig;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.asm.ClassMetadataReader;
import pro.gravit.launchserver.asm.ConfigGenerator;
import pro.gravit.launchserver.binary.BuildContext;
import pro.gravit.launchserver.binary.LauncherConfigurator;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JarHelper;
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
    @FunctionalInterface
    public interface Transformer {
        byte[] transform(byte[] input, String classname, BuildContext context);
    }

    public interface ASMTransformer extends Transformer {
        default byte[] transform(byte[] input, String classname, BuildContext context)
        {
            ClassReader reader = new ClassReader(input);
            ClassNode cn = new ClassNode();
            reader.accept(cn, 0);
            transform(cn, classname, context);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            cn.accept(writer);
            return writer.toByteArray();
        }
        void transform(ClassNode cn, String classname, BuildContext context);
    }
    public abstract static class ASMAnnotationFieldProcessor implements ASMTransformer
    {
        private final String desc;

        protected ASMAnnotationFieldProcessor(String desc) {
            this.desc = desc;
        }

        @Override
        public void transform(ClassNode cn, String classname, BuildContext context) {
            for(FieldNode fn : cn.fields)
            {
                if(fn.invisibleAnnotations == null || fn.invisibleAnnotations.isEmpty()) continue;
                AnnotationNode found = null;
                for(AnnotationNode an : fn.invisibleAnnotations)
                {
                    if(an == null) continue;
                    if(desc.equals(an.desc))
                    {
                        found = an;
                        break;
                    }
                }
                if(found != null)
                {
                    transformField(found, fn, cn, classname, context);
                }
            }
        }
        abstract public void transformField(AnnotationNode an, FieldNode fn, ClassNode cn, String classname, BuildContext context);
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
            hashs.put(fileName, SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, file));

            // Create zip entry and transfer contents
            output.putNextEntry(newEntry(fileName));
            IOHelper.transfer(file, output);

            // Return result
            return super.visitFile(file, attrs);
        }

        private ZipEntry newEntry(String fileName) {
            return newZipEntry(  targetDir + IOHelper.CROSS_SEPARATOR + fileName);
        }
    }
    public Set<String> blacklist = new HashSet<>();
    public List<Transformer> transformers = new ArrayList<>();

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
            new ClassReader(JarHelper.getClassBytes(AutogenConfig.class)).accept(cn, 0);
            LauncherConfigurator launcherConfigurator = new LauncherConfigurator(cn);
            ClassNode cn1 = new ClassNode();
            new ClassReader(JarHelper.getClassBytes(SecureAutogenConfig.class)).accept(cn1, 0);
            ConfigGenerator secureConfigurator = new ConfigGenerator(cn1);
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
            context.pushBytes(launcherConfigurator.getZipEntryPath(), launcherConfigurator.getBytecode(reader));
            context.pushBytes(secureConfigurator.getZipEntryPath(), secureConfigurator.getBytecode(reader));
            try (ZipInputStream input = new ZipInputStream(IOHelper.newInput(inputJar))) {
                ZipEntry e = input.getNextEntry();
                while (e != null) {
                    String filename = e.getName();
                    if (e.isDirectory() || blacklist.contains(filename) || context.fileList.contains(filename)) {
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
                        byte[] bytes = IOHelper.read(input);
                        bytes = transformClass(bytes, classname, context);
                        output.write(bytes);
                    } else
                        IOHelper.transfer(input, output);
                    context.fileList.add(filename);
                    e = input.getNextEntry();
                }
            }

            // map for guard
            Map<String, byte[]> runtime = new HashMap<>(256);
            if (server.buildHookManager.buildRuntime()) {
                // Write launcher guard dir
                IOHelper.walk(server.launcherBinary.runtimeDir, new RuntimeDirVisitor(output, runtime, server.launcherBinary.runtimeDir, "runtime"), false);
                IOHelper.walk(server.launcherBinary.guardDir,  new RuntimeDirVisitor(output, runtime, server.launcherBinary.guardDir, "guard"), false);
            }
            // Create launcher config file
            byte[] launcherConfigBytes;
            try (ByteArrayOutputStream configArray = IOHelper.newByteArrayOutput()) {
                try (HOutput configOutput = new HOutput(configArray)) {
                    new LauncherConfig(server.config.netty.address, server.publicKey, runtime, server.config.projectName)
                            .write(configOutput);
                }
                launcherConfigBytes = configArray.toByteArray();
            }

            // Write launcher config file
            output.putNextEntry(newZipEntry(Launcher.CONFIG_FILE));
            output.write(launcherConfigBytes);
        }
        reader.close();
        return outputJar;
    }

    public byte[] transformClass(byte[] bytes, String classname, BuildContext context)
    {
        byte[] result = bytes;
        ClassReader cr = null;
        ClassWriter writer = null;
        ClassNode cn = null;
        for(Transformer t : transformers)
        {
            if(t instanceof ASMTransformer)
            {
                ASMTransformer asmTransformer = (ASMTransformer) t;
                if(cn == null)
                {
                    cr = new ClassReader(result);
                    cn = new ClassNode();
                    cr.accept(cn, 0);
                }
                asmTransformer.transform(cn, classname, context);
                continue;
            }
            else if(cn != null)
            {
                writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                cn.accept(writer);
                result = writer.toByteArray();
            }
            byte[] old_result = result;
            result = t.transform(result, classname, context);
            if(old_result != result)
            {
                cr = null;
                cn = null;
            }
        }
        if(cn != null)
        {
            writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            cn.accept(writer);
            result = writer.toByteArray();
        }
        return result;
    }

    @Override
    public boolean allowDelete() {
        return true;
    }
}
