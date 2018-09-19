package ru.gravit.launchserver.binary;

import ru.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BuildContext {
    public final ZipOutputStream output;
    public final JAConfigurator config;

    public BuildContext(ZipOutputStream output, JAConfigurator config) {
        this.output = output;
        this.config = config;
    }
    public void pushFile(String filename,InputStream inputStream) throws IOException {
        ZipEntry zip = IOHelper.newZipEntry(filename);
        output.putNextEntry(zip);
        IOHelper.transfer(inputStream,output);
    }
    public void pushJarFile(JarInputStream input) throws IOException {
        ZipEntry e = input.getNextEntry();
        while (e != null) {
            output.putNextEntry(e);
            IOHelper.transfer(input,output);
            e = input.getNextEntry();
        }
    }
    public void pushJarFile(JarInputStream input, Set<String> blacklist) throws IOException {
        ZipEntry e = input.getNextEntry();
        while (e != null) {
            if(blacklist.contains(e.getName())){
                e = input.getNextEntry();
                continue;
            }
            output.putNextEntry(e);
            IOHelper.transfer(input,output);
            e = input.getNextEntry();
        }
    }
}
