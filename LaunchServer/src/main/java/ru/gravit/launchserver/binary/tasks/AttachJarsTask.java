package ru.gravit.launchserver.binary.tasks;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class AttachJarsTask implements LauncherBuildTask {
    private final LaunchServer srv;
    private final List<Path> jars;
    private final List<String> exclusions;

    public AttachJarsTask(LaunchServer srv) {
        this.srv = srv;
        jars = new ArrayList<>();
        exclusions = new ArrayList<>();
        exclusions.add("META-INF/MANIFEST.MF");
    }

    @Override
    public String getName() {
        return "AttachJars";
    }

    @Override
    public Path process(Path inputFile) throws IOException {
        Path outputFile = srv.launcherBinary.nextPath("attached");
        try (ZipInputStream input = IOHelper.newZipInput(inputFile);
             ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(outputFile))) {
            ZipEntry e = input.getNextEntry();
            while (e != null) {
                if (e.isDirectory()) {
                    e = input.getNextEntry();
                    continue;
                }
                output.putNextEntry(IOHelper.newZipEntry(e));
                IOHelper.transfer(input, output);
                e = input.getNextEntry();
            }
            attach(output, srv.launcherBinary.coreLibs);
            attach(output, jars);
        }
        return outputFile;
    }

    private void attach(ZipOutputStream output, List<Path> lst) throws IOException {
        for (Path p : lst) {
            LogHelper.debug("Attaching: " + p);
            try (ZipInputStream input = IOHelper.newZipInput(p)) {
                ZipEntry e = input.getNextEntry();
                while (e != null) {
                    String filename = e.getName();
                    if (exclusions.stream().noneMatch(filename::startsWith) && !e.isDirectory()) {
                        output.putNextEntry(IOHelper.newZipEntry(e));
                        IOHelper.transfer(input, output);
                    }
                    e = input.getNextEntry();
                }
            }
        }
    }

    @Override
    public boolean allowDelete() {
        return true;
    }

    public List<Path> getJars() {
        return jars;
    }

    public List<String> getExclusions() {
        return exclusions;
    }
}
