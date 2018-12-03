package ru.gravit.utils.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;

public class UnpackHelper {
    public static boolean unpack(URL resource, Path target) throws IOException {
        if (IOHelper.exists(target)) {
            if (matches(target, resource)) return false;
        }
        if (!IOHelper.exists(target))
            target.toFile().createNewFile();
        try (InputStream in = IOHelper.newInput(resource)) {
        	IOHelper.transfer(in, target, false);
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
}
