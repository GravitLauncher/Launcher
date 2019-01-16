package ru.gravit.launchserver.binary;

import ru.gravit.launchserver.binary.tasks.MainBuildTask;
import ru.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BuildContext {
    public final ZipOutputStream output;
    public final JAConfigurator config;
    public final MainBuildTask data;
    public final HashSet<String> fileList;


    public BuildContext(ZipOutputStream output, JAConfigurator config, MainBuildTask data) {
        this.output = output;
        this.config = config;
        this.data = data;
        fileList = new HashSet<>(1024);
    }

    public void pushFile(String filename, InputStream inputStream) throws IOException {
        ZipEntry zip = IOHelper.newZipEntry(filename);
        output.putNextEntry(zip);
        IOHelper.transfer(inputStream, output);
        fileList.add(filename);
    }

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

    public void pushJarFile(ZipInputStream input, Set<String> blacklist) throws IOException {
        ZipEntry e = input.getNextEntry();
        while (e != null) {
            if (blacklist.contains(e.getName())) {
                e = input.getNextEntry();
                continue;
            }
            output.putNextEntry(IOHelper.newZipEntry(e));
            IOHelper.transfer(input, output);
            fileList.add(e.getName());
            e = input.getNextEntry();
        }
    }
}
