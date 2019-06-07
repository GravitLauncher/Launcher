package pro.gravit.utils.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class UnpackHelper {
    public static boolean unpack(URL resource, Path target) throws IOException {
        if (IOHelper.isFile(target)) {
            if (matches(target, resource)) return false;
        }
        Files.deleteIfExists(target);
        IOHelper.createParentDirs(target);
        try (InputStream in = IOHelper.newInput(resource)) {
            IOHelper.transfer(in, target);
        }
        return true;
    }

    private static boolean matches(Path target, URL in) {
        try {
            return Arrays.equals(SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256, in),
                    SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256, target));
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean unpackZipNoCheck(URL resource, Path target) throws IOException {
        if (Files.isDirectory(target))
            return false;
        Files.deleteIfExists(target);
        Files.createDirectory(target);
        try (ZipInputStream input = IOHelper.newZipInput(resource)) {
            for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                if (entry.isDirectory())
                    continue; // Skip dirs
                // Unpack file
                IOHelper.transfer(input, target.resolve(IOHelper.toPath(entry.getName())));
            }
        }
        return true;
    }

    public static boolean unpackZipNoCheck(String resource, Path target) throws IOException {
        try {
            if (Files.isDirectory(target))
                return false;
            Files.deleteIfExists(target);
            Files.createDirectory(target);
            try (ZipInputStream input = IOHelper.newZipInput(IOHelper.getResourceURL(resource))) {
                for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                    if (entry.isDirectory())
                        continue; // Skip dirs
                    // Unpack file
                    IOHelper.transfer(input, target.resolve(IOHelper.toPath(entry.getName())));
                }
            }
            return true;
        } catch (NoSuchFileException e) {
            return true;
        }
    }
}
