package ru.gravit.launchserver.binary;

import static ru.gravit.utils.helper.IOHelper.newZipEntry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.ParseException;
import proguard.ProGuard;
import ru.gravit.launcher.AutogenConfig;
import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.asm.ClassMetadataReader;
import ru.gravit.launchserver.binary.tasks.LauncherBuildTask;
import ru.gravit.launchserver.binary.tasks.MainBuildTask;
import ru.gravit.launchserver.binary.tasks.ProGuardBuildTask;
import ru.gravit.launchserver.binary.tasks.UnpackBuildTask;
import ru.gravit.launchserver.manangers.hook.BuildHookManager.ZipBuildHook;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.SecurityHelper.DigestAlgorithm;
import ru.gravit.utils.helper.UnpackHelper;

public final class JARLauncherBinary extends LauncherBinary {
    //public ClassMetadataReader reader;
    public ArrayList<LauncherBuildTask> tasks;
    public JARLauncherBinary(LaunchServer server) throws IOException {
        super(server);
        tasks = new ArrayList<>();
        tasks.add(new UnpackBuildTask());
        tasks.add(new MainBuildTask());
        tasks.add(new ProGuardBuildTask());
        /*runtimeDir = server.dir.resolve(Launcher.RUNTIME_DIR);
        guardDir = server.dir.resolve(Launcher.GUARD_DIR);
        initScriptFile = runtimeDir.resolve(Launcher.INIT_SCRIPT_FILE);
        obfJar = server.dir.resolve(server.config.binaryName + "-obfPre.jar");
        obfOutJar = server.config.buildPostTransform.enabled ? server.dir.resolve(server.config.binaryName + "-obf.jar")
                : syncBinaryFile;
        cleanJar = server.dir.resolve(server.config.binaryName + "-clean.jar");
        reader = new ClassMetadataReader();
        UnpackHelper.unpack(IOHelper.getResourceURL("Launcher.jar"), cleanJar);
        reader.getCp().add(new JarFile(cleanJar.toFile()));*/
    }

    @Override
    public void build() throws IOException {
        // Build launcher binary
        LogHelper.info("Building launcher binary file");
        Path thisPath = null;
        for(LauncherBuildTask task : tasks)
        {
            LogHelper.subInfo("Task %s",task.getName());
            thisPath = task.process(thisPath);
            LogHelper.subInfo("Task %s processed",task.getName());
        }
        syncBinaryFile = thisPath;
        LogHelper.info("Build successful");

        // ProGuard

        /*for (Runnable r : server.buildHookManager.getPostProguardRunHooks())
            r.run();
        try (ZipInputStream input = new ZipInputStream(IOHelper.newInput(obfJar));
             ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(obfOutJar))) {
            ZipEntry e = input.getNextEntry();
            while (e != null) {
                String filename = e.getName();
                output.putNextEntry(IOHelper.newZipEntry(e));
                if (filename.endsWith(".class")) {
                    String classname = filename.replace('/', '.').substring(0, filename.length() - ".class".length());
                    byte[] bytes;
                    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048)) {
                        IOHelper.transfer(input, outputStream);
                        bytes = outputStream.toByteArray();
                    }
                    //bytes = server.buildHookManager.proGuardClassTransform(bytes, classname, this);
                    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                        IOHelper.transfer(inputStream, output);
                    }
                } else
                    IOHelper.transfer(input, output);
                e = input.getNextEntry();
            }
            for (ZipBuildHook h : server.buildHookManager.getProguardBuildHooks())
                h.build(output);
        }
        if (server.config.buildPostTransform.enabled)
            transformedBuild();*/
    }

    private void transformedBuild() throws IOException {
        /*List<String> cmd = new ArrayList<>(1);
        for (String v : server.config.buildPostTransform.script)
            cmd.add(CommonHelper.replace(v, "launcher-output", IOHelper.toAbsPathString(syncBinaryFile), "launcher-obf",
                    IOHelper.toAbsPathString(obfJar), "launcher-nonObf", IOHelper.toAbsPathString(binaryFile)));
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(IOHelper.toAbsPath(server.dir).toFile());
        builder.inheritIO();
        builder.command(cmd);
        Process proc = builder.start();
        try {
            LogHelper.debug("Transformer process return code: " + proc.waitFor());
        } catch (InterruptedException e) {
            LogHelper.error(e);
        }*/
    }
}
