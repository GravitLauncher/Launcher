package ru.gravit.launchserver.binary;

import static ru.gravit.utils.helper.IOHelper.newZipEntry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import ru.gravit.launcher.AutogenConfig;
import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.SecurityHelper.DigestAlgorithm;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launchserver.LaunchServer;
import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.ParseException;
import proguard.ProGuard;

public final class JARLauncherBinary extends LauncherBinary {

    public static final String[] guardFileList = {"Avanguard64.dll", "Avanguard32.dll", "wrapper64.exe", "wrapper32.exe"};

    private final class RuntimeDirVisitor extends SimpleFileVisitor<Path> {
        private final ZipOutputStream output;
        private final Map<String, byte[]> runtime;

        private RuntimeDirVisitor(ZipOutputStream output, Map<String, byte[]> runtime) {
            this.output = output;
            this.runtime = runtime;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            String dirName = IOHelper.toString(runtimeDir.relativize(dir));
            output.putNextEntry(newEntry(dirName + '/'));
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String fileName = IOHelper.toString(runtimeDir.relativize(file));
            runtime.put(fileName, SecurityHelper.digest(DigestAlgorithm.MD5, file));

            // Create zip entry and transfer contents
            output.putNextEntry(newEntry(fileName));
            IOHelper.transfer(file, output);

            // Return result
            return super.visitFile(file, attrs);
        }
    }

    // TODO: new native security wrapper and library...
    @SuppressWarnings("unused")
	private final class GuardDirVisitor extends SimpleFileVisitor<Path> {
        private final ZipOutputStream output;
        private final Map<String, byte[]> guard;

        private GuardDirVisitor(ZipOutputStream output, Map<String, byte[]> guard) {
            this.output = output;
            this.guard = guard;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            String dirName = IOHelper.toString(guardDir.relativize(dir));
            output.putNextEntry(newGuardEntry(dirName + '/'));
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String fileName = IOHelper.toString(guardDir.relativize(file));
            guard.put(fileName, SecurityHelper.digest(DigestAlgorithm.MD5, file));

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


    public final Path runtimeDir;
    public final Path guardDir;


    public final Path initScriptFile;


    public final Path obfJar;


    public JARLauncherBinary(LaunchServer server) throws IOException {
        super(server, server.dir.resolve(server.config.binaryName + ".jar"),
                server.dir.resolve(server.config.binaryName + "-obf.jar"));
        runtimeDir = server.dir.resolve(Launcher.RUNTIME_DIR);
        guardDir = server.dir.resolve("guard");
        initScriptFile = runtimeDir.resolve(Launcher.INIT_SCRIPT_FILE);
        obfJar = syncBinaryFile;
        tryUnpackRuntime();
    }

    @Override
    public void build() throws IOException {
        tryUnpackRuntime();
        tryUnpackGuard();

        // Build launcher binary
        LogHelper.info("Building launcher binary file");
        stdBuild();

        // ProGuard
        Configuration proguard_cfg = new Configuration();
        ConfigurationParser parser = new ConfigurationParser(
                server.proguardConf.confStrs.toArray(new String[0]),
                server.proguardConf.proguard.toFile(), System.getProperties());
        try {
            parser.parse(proguard_cfg);
            ProGuard proGuard = new ProGuard(proguard_cfg);
            proGuard.execute();
        } catch (ParseException e1) {
            e1.printStackTrace();
        }
        if (server.buildHookManager.isNeedPostProguardHook()) {
            Path obfPath = Paths.get(server.config.binaryName + "-obf.jar");
            Path tmpPath = Paths.get(server.config.binaryName + "-tmp.jar");
            IOHelper.move(obfPath, tmpPath);
            try (ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(obfPath))) {
                try (ZipInputStream input = new ZipInputStream(
                        IOHelper.newInput(tmpPath))) {
                    ZipEntry e = input.getNextEntry();
                    while (e != null) {
                        String filename = e.getName();
                        output.putNextEntry(e);
                        if (filename.endsWith(".class")) {
                            CharSequence classname = filename.replace('/', '.').subSequence(0,
                                    filename.length() - ".class".length());
                            byte[] bytes;
                            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048)) {
                                IOHelper.transfer(input, outputStream);
                                bytes = outputStream.toByteArray();
                            }
                            bytes = server.buildHookManager.proGuardClassTransform(bytes, classname);
                            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                                IOHelper.transfer(inputStream, output);
                            }
                        } else
                            IOHelper.transfer(input, output);
                        e = input.getNextEntry();
                    }
                }
            }
        }
        if (server.config.buildPostTransform.enabled)
        	transformedBuild();
    }

    private void transformedBuild() throws IOException {
    	List<String> cmd = new ArrayList<>(1);
    	server.config.buildPostTransform.script.forEach(v -> CommonHelper.replace(v, "launcher-output", IOHelper.toAbsPathString(syncBinaryFile), "launcher-obf", IOHelper.toAbsPathString(obfJar), "launcher-nonObf", IOHelper.toAbsPathString(binaryFile)));
    	ProcessBuilder builder = new ProcessBuilder();
    	builder.directory(IOHelper.toAbsPath(server.dir).toFile());
    	builder.inheritIO();
        builder.command(cmd);
    	Process proc = builder.start();
    	try {
			LogHelper.debug("Transformer process return code: " + proc.waitFor());
		} catch (InterruptedException e) {
			LogHelper.error(e);
		}
    }

    private void stdBuild() throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(binaryFile));
             JAConfigurator jaConfigurator = new JAConfigurator(AutogenConfig.class)) {
            BuildContext context = new BuildContext(output, jaConfigurator);
            server.buildHookManager.preHook(context);
            jaConfigurator.setAddress(server.config.getAddress());
            jaConfigurator.setPort(server.config.port);
            jaConfigurator.setProjectName(server.config.projectName);
            jaConfigurator.setSecretKey(SecurityHelper.randomStringAESKey());
            jaConfigurator.setClientPort(32148 + SecurityHelper.newRandom().nextInt(512));
            jaConfigurator.setUsingWrapper(server.config.isUsingWrapper);
            jaConfigurator.setDownloadJava(server.config.isDownloadJava);
            server.buildHookManager.registerAllClientModuleClass(jaConfigurator);
            try (ZipInputStream input = new ZipInputStream(
                    IOHelper.newInput(IOHelper.getResourceURL("Launcher.jar")))) {
                ZipEntry e = input.getNextEntry();
                while (e != null) {
                    String filename = e.getName();
                    if (server.buildHookManager.isContainsBlacklist(filename)) {
                        e = input.getNextEntry();
                        continue;
                    }
                    try {
                        output.putNextEntry(e);
                    } catch (ZipException ex) {
                        LogHelper.error(ex);
                        e = input.getNextEntry();
                        continue;
                    }
                    if (filename.endsWith(".class")) {
                        CharSequence classname = filename.replace('/', '.').subSequence(0,
                                filename.length() - ".class".length());
                        byte[] bytes;
                        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048)) {
                            IOHelper.transfer(input, outputStream);
                            bytes = outputStream.toByteArray();
                        }
                        bytes = server.buildHookManager.classTransform(bytes, classname);
                        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                            IOHelper.transfer(inputStream, output);
                        }
                    } else
                        IOHelper.transfer(input, output);
                    // }
                    e = input.getNextEntry();
                }
            }
            // write additional classes
            for (Entry<String, byte[]> ent : server.buildHookManager.getIncludeClass().entrySet()) {
                output.putNextEntry(newZipEntry(ent.getKey().replace('.', '/').concat(".class")));
                output.write(server.buildHookManager.classTransform(ent.getValue(), ent.getKey()));
            }
            // map for guard
            Map<String, byte[]> runtime = new HashMap<>(256);
            if (server.buildHookManager.buildRuntime()) {
                // Verify has init script file
                if (!IOHelper.isFile(initScriptFile))
                    throw new IOException(String.format("Missing init script file ('%s')", Launcher.INIT_SCRIPT_FILE));
                // Write launcher guard dir
                IOHelper.walk(runtimeDir, new RuntimeDirVisitor(output, runtime), false);
                //IOHelper.walk(guardDir, new GuardDirVisitor(output, runtime), false);
            }
            // Create launcher config file
            byte[] launcherConfigBytes;
            try (ByteArrayOutputStream configArray = IOHelper.newByteArrayOutput()) {
                try (HOutput configOutput = new HOutput(configArray)) {
                    new LauncherConfig(server.config.getAddress(), server.config.port, server.publicKey, runtime)
                            .write(configOutput);
                }
                launcherConfigBytes = configArray.toByteArray();
            }

            // Write launcher config file
            output.putNextEntry(newZipEntry(Launcher.CONFIG_FILE));
            output.write(launcherConfigBytes);
            ZipEntry e = newZipEntry(jaConfigurator.getZipEntryPath());
            output.putNextEntry(e);
            jaConfigurator.compile();
            output.write(jaConfigurator.getBytecode());
            server.buildHookManager.postHook(context);
        } catch (CannotCompileException | NotFoundException e) {
            LogHelper.error(e);
        }
    }


    public void tryUnpackRuntime() throws IOException {
        // Verify is guard dir unpacked
        if (IOHelper.isDir(runtimeDir))
            return; // Already unpacked

        // Unpack launcher guard files
        Files.createDirectory(runtimeDir);
        LogHelper.info("Unpacking launcher runtime files");
        try (ZipInputStream input = IOHelper.newZipInput(IOHelper.getResourceURL("runtime.zip"))) {
            for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                if (entry.isDirectory())
                    continue; // Skip dirs

                // Unpack guard file
                IOHelper.transfer(input, runtimeDir.resolve(IOHelper.toPath(entry.getName())));
            }
        }
    }

    public void tryUnpackGuard() throws IOException {
        // Verify is guard dir unpacked
        if (IOHelper.isDir(guardDir))
            return; // Already unpacked

        // Unpack launcher guard files
        Files.createDirectory(guardDir);
        LogHelper.info("Unpacking launcher native guard files");
        try (ZipInputStream input = IOHelper.newZipInput(IOHelper.getResourceURL("guard.zip"))) {
            for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                if (entry.isDirectory())
                    continue; // Skip dirs

                // Unpack guard file
                IOHelper.transfer(input, guardDir.resolve(IOHelper.toPath(entry.getName())));
            }
        }
    }
}
