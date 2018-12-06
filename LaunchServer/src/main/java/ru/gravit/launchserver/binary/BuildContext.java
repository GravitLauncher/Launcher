package ru.gravit.launchserver.binary;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import ru.gravit.utils.helper.IOHelper;

public class BuildContext {
    public final ZipOutputStream output;
    public final JAConfigurator config;
    public final JARLauncherBinary data;

    public BuildContext(ZipOutputStream output, JAConfigurator config, JARLauncherBinary data) {
        this.output = output;
        this.config = config;
        this.data = data;
    }

    public void pushFile(String filename, InputStream inputStream) throws IOException {
        ZipEntry zip = IOHelper.newZipEntry(filename);
        output.putNextEntry(zip);
        IOHelper.transfer(inputStream, output);
    }

    public void pushJarFile(ZipInputStream input) throws IOException {
        ZipEntry e = input.getNextEntry();
        while (e != null) {
            output.putNextEntry(e);
            IOHelper.transfer(input, output);
            e = input.getNextEntry();
        }
    }

    public void pushJarFile(ZipInputStream input, Set<String> blacklist) throws IOException {
        ZipEntry e = input.getNextEntry();
        while (e != null) {
            if (blacklist.contains(e.getName())) {
                e = input.getNextEntry();
                continue;
            }
            output.putNextEntry(e);
            IOHelper.transfer(input, output);
            e = input.getNextEntry();
        }
    }
}
