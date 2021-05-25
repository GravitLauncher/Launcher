package pro.gravit.launchserver.binary.tasks;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.IOHelper;

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
        exclusions.add("META-INF");
        exclusions.add("module-info.class");
        exclusions.add("LICENSE");
        exclusions.add("LICENSE.txt");
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
            attach(output, inputFile, srv.launcherBinary.coreLibs);
            attach(output, inputFile, jars);
        }
        return outputFile;
    }

    private void attach(ZipOutputStream output, Path inputFile, List<Path> lst) throws IOException {
        for (Path p : lst) {
            AdditionalFixesApplyTask.apply(inputFile, p, output, srv, (e) -> filter(e.getName()), false);
        }
    }

    private boolean filter(String name) {
        if (name.startsWith("META-INF/services")) return false;
        return exclusions.stream().anyMatch(name::startsWith);
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
