package pro.gravit.launchserver.binary.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.asm.ClassMetadataReader;
import pro.gravit.launchserver.asm.InjectClassAcceptor;
import pro.gravit.launchserver.asm.SafeClassWriter;
import pro.gravit.launchserver.binary.BuildContext;
import pro.gravit.launchserver.binary.PipelineContext;
import pro.gravit.utils.HookException;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.CertificateEncodingException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

public class MainBuildTask implements LauncherBuildTask {
    public final ClassMetadataReader reader;
    public final Set<String> blacklist = new HashSet<>();
    public final IOHookSet<BuildContext> preBuildHook = new IOHookSet<>();
    public final IOHookSet<BuildContext> postBuildHook = new IOHookSet<>();
    private final LaunchServer server;
    private transient final Logger logger = LogManager.getLogger();

    public MainBuildTask(LaunchServer srv) {
        server = srv;
        reader = new ClassMetadataReader();
    }

    @Override
    public String getName() {
        return "MainBuild";
    }

    @Override
    public Path process(PipelineContext pipelineContext) throws IOException {
        Path inputJar = pipelineContext.getLastest();
        Path outputJar = pipelineContext.makeTempPath("main", ".jar");
        try (ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(outputJar))) {
            BuildContext context = new BuildContext(pipelineContext, output, reader.getCp(), this, server.launcherBinary.runtimeDir);
            initProps(context);
            preBuildHook.hook(context);
            context.properties.put("launcher.legacymodules", context.legacyClientModules.stream().map(e -> Type.getObjectType(e.replace('.', '/'))).collect(Collectors.toList()));
            context.properties.put("launcher.modules", context.clientModules.stream().map(e -> Type.getObjectType(e.replace('.', '/'))).collect(Collectors.toList()));
            postInitProps(context);
            reader.getCp().add(new JarFile(inputJar.toFile()));
            for (Path e : server.launcherBinary.coreLibs) {
                reader.getCp().add(new JarFile(e.toFile()));
            }
            context.pushJarFile(inputJar, (e) -> blacklist.contains(e.getName()) || e.getName().startsWith("pro/gravit/launcher/runtime/debug/"), (e) -> true);

            // map for guard
            Map<String, byte[]> runtime = new HashMap<>(256);
            // Write launcher guard dir
            if (server.config.launcher.encryptRuntime) {
                String runtimeEncryptKey = context.pipelineContext.getProperty("runtimeEncryptKey");
                context.pushEncryptedDir(context.getRuntimeDir(), Launcher.RUNTIME_DIR, runtimeEncryptKey, runtime, false);
            } else {
                context.pushDir(context.getRuntimeDir(), Launcher.RUNTIME_DIR, runtime, false);
            }
            if(context.isDeleteRuntimeDir()) {
                IOHelper.deleteDir(context.getRuntimeDir(), true);
            }

            LauncherConfig launcherConfig = new LauncherConfig(server.config.netty.address, server.keyAgreementManager.ecdsaPublicKey, server.keyAgreementManager.rsaPublicKey, runtime, server.config.projectName);
            context.pushFile(Launcher.CONFIG_FILE, launcherConfig);
            postBuildHook.hook(context);
        }
        reader.close();
        return outputJar;
    }

    protected void postInitProps(BuildContext context) {
        List<byte[]> certificates = Arrays.stream(server.certificateManager.trustManager.getTrusted()).map(e -> {
            try {
                return e.getEncoded();
            } catch (CertificateEncodingException e2) {
                logger.error("Certificate encoding failed", e2);
                return new byte[0];
            }
        }).collect(Collectors.toList());
        if (!server.config.sign.enabled) {
            CertificateAutogenTask task = server.launcherBinary.getTaskByClass(CertificateAutogenTask.class).get();
            try {
                certificates.add(task.certificate.getEncoded());
            } catch (CertificateEncodingException e) {
                throw new InternalError(e);
            }
        }
        context.properties.put("launchercore.certificates", certificates);
    }

    protected void initProps(BuildContext context) {
        context.properties.clear();
        context.properties.put("launcher.address", server.config.netty.address);
        context.properties.put("launcher.projectName", server.config.projectName);
        context.properties.put("runtimeconfig.secretKeyClient", SecurityHelper.randomStringAESKey());
        context.properties.put("launcher.port", 32148 + SecurityHelper.newRandom().nextInt(512));
        context.properties.put("launchercore.env", server.config.env);
        context.properties.put("launcher.memory", server.config.launcher.memoryLimit);
        context.properties.put("launcher.customJvmOptions", server.config.launcher.customJvmOptions);
        if (server.config.launcher.encryptRuntime) {
            String runtimeEncryptKey = SecurityHelper.randomStringToken();
            context.pipelineContext.putProperty("runtimeEncryptKey", runtimeEncryptKey);
            context.properties.put("runtimeconfig.runtimeEncryptKey", runtimeEncryptKey);
        }
        context.properties.put("launcher.certificatePinning", server.config.launcher.certificatePinning);
        String checkClientSecret = SecurityHelper.randomStringToken();
        context.pipelineContext.putProperty("checkClientSecret", checkClientSecret);
        String launcherSalt = SecurityHelper.randomStringToken();
        byte[] launcherSecureHash = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256,
                checkClientSecret.concat(".").concat(launcherSalt));
        context.properties.put("runtimeconfig.secureCheckHash", Base64.getEncoder().encodeToString(launcherSecureHash));
        context.properties.put("runtimeconfig.secureCheckSalt", launcherSalt);
        String unlockSecret = SecurityHelper.randomStringToken();
        context.pipelineContext.putProperty("unlockSecret", unlockSecret);
        context.properties.put("runtimeconfig.unlockSecret", unlockSecret);
    }

    public byte[] transformClass(byte[] bytes, String classname, BuildContext context) {
        byte[] result = bytes;
        ClassWriter writer;
        ClassNode cn = null;
        for (Transformer t : context.transformers) {
            if (t instanceof ASMTransformer asmTransformer) {
                if (cn == null) {
                    ClassReader cr = new ClassReader(result);
                    cn = new ClassNode();
                    cr.accept(cn, 0);
                }
                asmTransformer.transform(cn, classname, context);
                continue;
            } else if (cn != null) {
                writer = new SafeClassWriter(reader, 0);
                cn.accept(writer);
                result = writer.toByteArray();
            }
            byte[] old_result = result;
            result = t.transform(result, classname, context);
            if (old_result != result) {
                cn = null;
            }
        }
        if (cn != null) {
            writer = new SafeClassWriter(reader, 0);
            cn.accept(writer);
            result = writer.toByteArray();
        }
        return result;
    }

    @FunctionalInterface
    public interface Transformer {
        byte[] transform(byte[] input, String classname, BuildContext context);
    }

    public interface ASMTransformer extends Transformer {
        default byte[] transform(byte[] input, String classname, BuildContext context) {
            ClassReader reader = new ClassReader(input);
            ClassNode cn = new ClassNode();
            reader.accept(cn, 0);
            transform(cn, classname, context);
            SafeClassWriter writer = new SafeClassWriter(context.task.reader, 0);
            cn.accept(writer);
            return writer.toByteArray();
        }

        void transform(ClassNode cn, String classname, BuildContext context);
    }

    public static class IOHookSet<R> {
        public final Set<IOHook<R>> list = new HashSet<>();

        public void registerHook(IOHook<R> hook) {
            list.add(hook);
        }

        public boolean unregisterHook(IOHook<R> hook) {
            return list.remove(hook);
        }

        /**
         * @param context custom param
         *                False to continue
         * @throws HookException The hook may return the error text throwing this exception
         */
        public void hook(R context) throws HookException, IOException {
            for (IOHook<R> hook : list) {
                hook.hook(context);
            }
        }

        @FunctionalInterface
        public interface IOHook<R> {
            /**
             * @param context custom param
             *                False to continue processing hook
             * @throws HookException The hook may return the error text throwing this exception
             */
            void hook(R context) throws HookException, IOException;
        }
    }

    public abstract static class ASMAnnotationFieldProcessor implements ASMTransformer {
        private final String desc;

        protected ASMAnnotationFieldProcessor(String desc) {
            this.desc = desc;
        }

        @Override
        public void transform(ClassNode cn, String classname, BuildContext context) {
            for (FieldNode fn : cn.fields) {
                if (fn.invisibleAnnotations == null || fn.invisibleAnnotations.isEmpty()) continue;
                AnnotationNode found = null;
                for (AnnotationNode an : fn.invisibleAnnotations) {
                    if (an == null) continue;
                    if (desc.equals(an.desc)) {
                        found = an;
                        break;
                    }
                }
                if (found != null) {
                    transformField(found, fn, cn, classname, context);
                }
            }
        }

        abstract public void transformField(AnnotationNode an, FieldNode fn, ClassNode cn, String classname, BuildContext context);
    }
}
