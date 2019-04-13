package ru.gravit.utils.helper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarHelper {
    @FunctionalInterface
    interface ZipWalkCallback
    {
        void process(ZipInputStream input, ZipEntry e);
    }
    @FunctionalInterface
    interface JarWalkCallback
    {
        void process(ZipInputStream input, ZipEntry e, String fullClassName, String clazz);
    }
    public static void zipWalk(ZipInputStream input, ZipWalkCallback callback) throws IOException
    {
        ZipEntry e = input.getNextEntry();
        while (e != null)
        {
            callback.process(input, e);
            e = input.getNextEntry();
        }
    }
    public static void jarWalk(JarInputStream input, JarWalkCallback callback) throws IOException
    {
        zipWalk(input, (in, e) -> {
            String filename = e.getName();
            if(filename.endsWith(".class"))
            {
                String classFull = filename.replaceAll("/", ".").substring(0,
                        filename.length() - ".class".length());
                String clazz = classFull.substring(classFull.lastIndexOf('.'));
                callback.process(in,e, classFull, clazz);
            }
        });
    }
    public static Map<String, String> jarMap(JarInputStream input, boolean overwrite) throws IOException
    {
        Map<String, String> map = new HashMap<>();
        jarMap(input, map, overwrite);
        return map;
    }
    public static void jarMap(JarInputStream input, Map<String, String> map, boolean overwrite) throws IOException
    {
        jarWalk(input, (in, e, classFull, clazz) -> {
            if(overwrite) map.put(clazz, classFull);
            else map.putIfAbsent(clazz, classFull);
        });
    }
    public static Map<String, String> jarMap(Path file, boolean overwrite) throws IOException
    {
        try(JarInputStream inputStream = new JarInputStream(IOHelper.newZipInput(file)))
        {
            return jarMap(inputStream,overwrite);
        }
    }
    public static void jarMap(Path file, Map<String, String> map, boolean overwrite) throws IOException
    {
        try(JarInputStream inputStream = new JarInputStream(IOHelper.newZipInput(file)))
        {
            jarMap(inputStream, map,overwrite);
        }
    }
    public static Map<String, String> jarMap(Class clazz, boolean overwrite) throws IOException
    {
        Path file = IOHelper.getCodeSource(clazz);
        return jarMap(file, overwrite);
    }
    public static void jarMap(Class clazz, Map<String, String> map, boolean overwrite) throws IOException
    {
        Path file = IOHelper.getCodeSource(clazz);
        jarMap(file, map, overwrite);
    }
}
