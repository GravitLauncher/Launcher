package ru.gravit.utils.helper;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;

public class UnpackHelper {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean unpack(URL resource, Path target) throws IOException {
        byte[] orig = IOHelper.read(resource);
        if(IOHelper.exists(target))
        {
            if(matches(target,orig)) return false;
        }
        if (!IOHelper.exists(target))
            target.toFile().createNewFile();
        IOHelper.transfer(orig,target,false);
        return true;
    }
    private static boolean matches(Path target, byte[] in) {
        try {
            return Arrays.equals(SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256, in),
                    SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256, target));
        } catch (IOException e) {
            return false;
        }
    }
}
