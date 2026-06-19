package pro.gravit.utils.helper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarHelper {
    public static String getClassFile(Class<?> clazz) {
        return getClassFile(clazz.getName());
    }

    public static String getClassFile(String classname) {
        return classname.replace('.', '/').concat(".class");
    }

    public static byte[] getClassBytes(Class<?> clazz) throws IOException {
        return getClassBytes(clazz, clazz.getClassLoader());
    }

    public static byte[] getClassBytes(Class<?> clazz, ClassLoader classLoader) throws IOException {
        return IOHelper.read(classLoader.getResourceAsStream(getClassFile(clazz)));
    }

    public static InputStream getClassBytesStream(Class<?> clazz) {
        return getClassBytesStream(clazz, clazz.getClassLoader());
    }

    public static InputStream getClassBytesStream(Class<?> clazz, ClassLoader classLoader) {
        return classLoader.getResourceAsStream(getClassFile(clazz));
    }

    public static byte[] getClassFromJar(String name, Path file) throws IOException {
        String filename = getClassFile(name);
        try (ZipInputStream inputStream = IOHelper.newZipInput(file)) {
            ZipEntry entry = inputStream.getNextEntry();
            while (entry != null) {
                if (entry.getName().equals(filename)) {
                    return IOHelper.read(inputStream);
                }
                entry = inputStream.getNextEntry();
            }
        }
        throw new FileNotFoundException(filename);
    }

}
